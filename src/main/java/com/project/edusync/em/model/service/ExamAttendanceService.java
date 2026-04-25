package com.project.edusync.em.model.service;

import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.common.exception.BadRequestException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.em.model.dto.request.ExamAttendanceFinalizeRequestDTO;
import com.project.edusync.em.model.dto.request.ExamAttendanceMarkEntryDTO;
import com.project.edusync.em.model.dto.request.ExamAttendanceMarkRequestDTO;
import com.project.edusync.em.model.dto.response.ExamAttendanceFinalizeResponseDTO;
import com.project.edusync.em.model.dto.response.ExamAttendanceMarkResponseDTO;
import com.project.edusync.em.model.dto.response.ExamRoomStudentResponseDTO;
import com.project.edusync.em.model.dto.response.InvigilatorRoomResponseDTO;
import com.project.edusync.em.model.entity.ExamAttendance;
import com.project.edusync.em.model.entity.ExamEntryDecision;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.enums.ExamAttendanceStatus;
import com.project.edusync.em.model.repository.ExamAttendanceRepository;
import com.project.edusync.em.model.repository.ExamEntryDecisionRepository;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.InvigilationRepository;
import com.project.edusync.em.model.repository.SeatAllocationRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamAttendanceService {

    private final AuthUtil authUtil;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final InvigilationRepository invigilationRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final ExamAttendanceRepository examAttendanceRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final RoomRepository roomRepository;
    private final ExamEntryDecisionRepository examEntryDecisionRepository;
    private final ExamControllerAccessService examControllerAccessService;

    public List<InvigilatorRoomResponseDTO> getAssignedRoomsForCurrentInvigilator() {
        Long staffId = resolveCurrentStaffId();
        return invigilationRepository.findAssignedRoomsByStaffId(staffId)
            .stream()
            .map(r -> InvigilatorRoomResponseDTO.builder()
                .examScheduleId(r.getExamScheduleId())
                .roomId(r.getRoomId())
                .roomName(r.getRoomName())
                .subjectName(r.getSubjectName())
                .className(r.getClassName())
                .examDate(r.getExamDate())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .build())
            .toList();
    }

    public List<ExamRoomStudentResponseDTO> getRoomAttendanceRoster(Long roomId, Long examScheduleId) {
        Long staffId = resolveCurrentStaffId();
        validateInvigilatorAssignment(staffId, examScheduleId, roomId);

        ExamSchedule schedule = findScheduleWithTimeslot(examScheduleId);
        LocalDateTime slotStart = toScheduleStart(schedule);
        LocalDateTime slotEnd = toScheduleEnd(schedule);

        return seatAllocationRepository.findExamRoomStudentsByTimeWindow(roomId, slotStart, slotEnd)
            .stream()
            .map(s -> ExamRoomStudentResponseDTO.builder()
                .studentId(s.getStudentId())
                .studentName(buildName(s.getFirstName(), s.getLastName()))
                .rollNo(s.getRollNo())
                .className(s.getClassName())
                .subjectName(s.getSubjectName())
                .seatNumber(resolveSeatNumber(s.getSeatNumber(), s.getRowNumber(), s.getColumnNumber()))
                .attendanceStatus(normalizeStoredStatus(s.getAttendanceStatus()))
                .malpractice(Boolean.TRUE.equals(s.getMalpractice()) || s.getAttendanceStatus() == ExamAttendanceStatus.MALPRACTICE)
                .finalized(Boolean.TRUE.equals(s.getFinalized()))
                .entryAllowed(Boolean.TRUE.equals(s.getEntryAllowed()))
                .build())
            .toList();
    }

    @Transactional
    public ExamAttendanceMarkResponseDTO markAttendance(ExamAttendanceMarkRequestDTO request) {
        Long staffId = resolveCurrentStaffId();
        validateInvigilatorAssignment(staffId, request.getExamScheduleId(), request.getRoomId());
        ExamSchedule anchorSchedule = findScheduleWithTimeslot(request.getExamScheduleId());
        LocalDateTime slotStart = toScheduleStart(anchorSchedule);
        LocalDateTime slotEnd = toScheduleEnd(anchorSchedule);

        Set<Long> roomStudentIds = new HashSet<>(seatAllocationRepository
            .findExamRoomStudentIdsByTimeWindow(request.getRoomId(), slotStart, slotEnd));
        if (roomStudentIds.isEmpty()) {
            throw new BadRequestException("No seat allocations found for this room and exam schedule");
        }

        Set<Long> requestedStudentIds = request.getAttendances().stream().map(ExamAttendanceMarkEntryDTO::getStudentId).collect(Collectors.toSet());
        if (requestedStudentIds.size() != request.getAttendances().size()) {
            throw new BadRequestException("Duplicate student entries are not allowed in one mark request");
        }
        if (!roomStudentIds.containsAll(requestedStudentIds)) {
            throw new BadRequestException("One or more students are not allocated in this exam room");
        }

        Map<Long, Long> scheduleByStudent = resolveScheduleByStudent(request.getRoomId(), slotStart, slotEnd, requestedStudentIds);
        Set<Long> scheduleIds = new HashSet<>(scheduleByStudent.values());
        ensureNotFinalized(scheduleIds, request.getRoomId());

        Map<String, ExamAttendance> existingByStudentId = examAttendanceRepository
            .findByExamScheduleIdsAndStudentIds(scheduleIds, requestedStudentIds)
            .stream()
            .collect(Collectors.toMap(
                e -> attendanceKey(e.getExamSchedule().getId(), e.getStudent().getId()),
                Function.identity(),
                (a, b) -> a));

        Map<String, Boolean> entryAllowedByKey = examEntryDecisionRepository
            .findByExamScheduleIdsAndStudentIds(scheduleIds, requestedStudentIds)
            .stream()
            .collect(Collectors.toMap(
                e -> attendanceKey(e.getExamSchedule().getId(), e.getStudent().getId()),
                ExamEntryDecision::isAllowed,
                (left, right) -> right));

        Map<Long, ExamSchedule> schedulesById = examScheduleRepository.findAllById(scheduleIds).stream()
            .collect(Collectors.toMap(ExamSchedule::getId, Function.identity()));
        if (schedulesById.size() != scheduleIds.size()) {
            throw new BadRequestException("Seat allocations reference missing exam schedules");
        }
        Room room = roomRepository.findById(request.getRoomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));
        Staff marker = staffRepository.findById(staffId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", staffId));

        Map<Long, Student> studentEntities = studentRepository.findAllById(requestedStudentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));

        List<ExamAttendance> toSave = new ArrayList<>(request.getAttendances().size());
        for (ExamAttendanceMarkEntryDTO entry : request.getAttendances()) {
            Long scheduleId = scheduleByStudent.get(entry.getStudentId());
            if (scheduleId == null) {
                throw new BadRequestException("One or more students are not allocated in this exam room");
            }
            String key = attendanceKey(scheduleId, entry.getStudentId());
            ExamAttendance attendance = existingByStudentId.get(key);
            if (attendance != null && !Objects.equals(attendance.getRoom().getId(), request.getRoomId())) {
                throw new BadRequestException("Inconsistent attendance data found for this room");
            }
            if (attendance == null) {
                Student student = studentEntities.get(entry.getStudentId());
                if (student == null) {
                    throw new ResourceNotFoundException("Student", "id", entry.getStudentId());
                }
                ExamSchedule mappedSchedule = schedulesById.get(scheduleId);
                if (mappedSchedule == null) {
                    throw new ResourceNotFoundException("ExamSchedule", "id", scheduleId);
                }
                attendance = ExamAttendance.builder()
                    .examSchedule(mappedSchedule)
                    .student(student)
                    .room(room)
                    .build();
            }
            ExamAttendanceStatus normalizedStatus = normalizeRequestedStatus(entry);
            boolean entryAllowed = entryAllowedByKey.getOrDefault(key, true);
            if (!entryAllowed && normalizedStatus == ExamAttendanceStatus.PRESENT) {
                throw new BadRequestException("Student is blocked from this exam. Mark as absent or allow entry first");
            }
            boolean malpractice = normalizeMalpractice(entry);
            if (malpractice && normalizedStatus == ExamAttendanceStatus.ABSENT) {
                throw new BadRequestException("Malpractice can only be marked for present students");
            }
            attendance.setStatus(normalizedStatus);
            attendance.setMalpracticeReported(malpractice);
            attendance.setMarkedBy(marker);
            attendance.setTimestamp(LocalDateTime.now());
            attendance.setFinalized(false);
            toSave.add(attendance);
        }

        examAttendanceRepository.saveAll(toSave);

        return ExamAttendanceMarkResponseDTO.builder()
            .savedCount(toSave.size())
            .finalized(false)
            .build();
    }

    @Transactional
    public ExamAttendanceFinalizeResponseDTO finalizeAttendance(ExamAttendanceFinalizeRequestDTO request) {
        Long staffId = resolveCurrentStaffId();
        validateInvigilatorAssignment(staffId, request.getExamScheduleId(), request.getRoomId());

        ExamSchedule schedule = findScheduleWithTimeslot(request.getExamScheduleId());
        LocalDateTime slotStart = toScheduleStart(schedule);
        LocalDateTime slotEnd = toScheduleEnd(schedule);

        if (LocalDateTime.now().isBefore(slotEnd)) {
            throw new BadRequestException("Attendance can be finalized only after exam end time");
        }

        List<Long> roomStudentIds = seatAllocationRepository.findExamRoomStudentIdsByTimeWindow(request.getRoomId(), slotStart, slotEnd);
        if (roomStudentIds.isEmpty()) {
            throw new BadRequestException("No seat allocations found for this room and exam schedule");
        }

        Map<Long, Long> scheduleByStudent = resolveScheduleByStudent(request.getRoomId(), slotStart, slotEnd, new HashSet<>(roomStudentIds));
        Set<Long> scheduleIds = new HashSet<>(scheduleByStudent.values());
        ensureNotFinalized(scheduleIds, request.getRoomId());

        Map<String, ExamAttendance> existingByStudentId = examAttendanceRepository
            .findByExamScheduleIdsAndStudentIds(scheduleIds, roomStudentIds)
            .stream()
            .collect(Collectors.toMap(
                e -> attendanceKey(e.getExamSchedule().getId(), e.getStudent().getId()),
                Function.identity(),
                (a, b) -> a));

        Staff marker = staffRepository.findById(staffId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", staffId));
        Room room = roomRepository.findById(request.getRoomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));

        Map<Long, ExamSchedule> schedulesById = examScheduleRepository.findAllById(scheduleIds).stream()
            .collect(Collectors.toMap(ExamSchedule::getId, Function.identity()));
        if (schedulesById.size() != scheduleIds.size()) {
            throw new BadRequestException("Seat allocations reference missing exam schedules");
        }

        List<ExamAttendance> toSave = new ArrayList<>(roomStudentIds.size());
        int alreadyMarked = 0;
        int autoMarkedAbsent = 0;

        Set<Long> missingStudentIds = new HashSet<>(roomStudentIds);
        missingStudentIds.removeAll(existingByStudentId.values().stream().map(e -> e.getStudent().getId()).collect(Collectors.toSet()));
        Map<Long, Student> missingStudents = missingStudentIds.isEmpty()
            ? Collections.emptyMap()
            : studentRepository.findAllById(missingStudentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));

        for (Long studentId : roomStudentIds) {
            Long scheduleId = scheduleByStudent.get(studentId);
            if (scheduleId == null) {
                throw new BadRequestException("One or more students are not allocated in this exam room");
            }
            String key = attendanceKey(scheduleId, studentId);
            ExamAttendance attendance = existingByStudentId.get(key);
            if (attendance == null) {
                Student student = missingStudents.get(studentId);
                if (student == null) {
                    throw new ResourceNotFoundException("Student", "id", studentId);
                }
                ExamSchedule mappedSchedule = schedulesById.get(scheduleId);
                if (mappedSchedule == null) {
                    throw new ResourceNotFoundException("ExamSchedule", "id", scheduleId);
                }
                attendance = ExamAttendance.builder()
                    .examSchedule(mappedSchedule)
                    .student(student)
                    .room(room)
                    .status(ExamAttendanceStatus.ABSENT)
                    .malpracticeReported(false)
                    .markedBy(marker)
                    .timestamp(LocalDateTime.now())
                    .finalized(true)
                    .build();
                autoMarkedAbsent++;
            } else {
                alreadyMarked++;
                attendance.setFinalized(true);
            }
            toSave.add(attendance);
        }

        examAttendanceRepository.saveAll(toSave);

        return ExamAttendanceFinalizeResponseDTO.builder()
            .totalStudents(roomStudentIds.size())
            .alreadyMarked(alreadyMarked)
            .autoMarkedAbsent(autoMarkedAbsent)
            .finalized(true)
            .build();
    }

    private Long resolveCurrentStaffId() {
        Long userId = authUtil.getCurrentUserId();
        return staffRepository.findByUserProfile_User_Id(userId)
            .map(Staff::getId)
            .orElseThrow(() -> new BadRequestException("Logged-in user is not mapped to staff"));
    }

    public Long resolveCurrentStaffIdForPrivilegedAction() {
        return resolveCurrentStaffId();
    }

    private void validateInvigilatorAssignment(Long staffId, Long examScheduleId, Long roomId) {
        if (examControllerAccessService.canAccessSchedule(examScheduleId)) {
            return;
        }
        if (!invigilationRepository.existsByExamScheduleIdAndRoom_IdAndStaffId(examScheduleId, roomId, staffId)) {
            throw new BadRequestException("You are not assigned as invigilator for this room and exam");
        }
    }

    private void ensureNotFinalized(Set<Long> examScheduleIds, Long roomId) {
        if (examScheduleIds.isEmpty()) {
            return;
        }
        if (examAttendanceRepository.existsByExamScheduleIdInAndRoomIdAndFinalizedTrue(examScheduleIds, roomId)) {
            throw new BadRequestException("Attendance already finalized for this room");
        }
    }

    private Map<Long, Long> resolveScheduleByStudent(Long roomId,
                                                     LocalDateTime slotStart,
                                                     LocalDateTime slotEnd,
                                                     Set<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return seatAllocationRepository
            .findStudentSchedulesInRoomByTimeWindowAndStudentIds(roomId, slotStart, slotEnd, studentIds)
            .stream()
            .collect(Collectors.toMap(
                SeatAllocationRepository.RoomStudentScheduleProjection::getStudentId,
                SeatAllocationRepository.RoomStudentScheduleProjection::getExamScheduleId,
                (left, right) -> {
                    if (Objects.equals(left, right)) {
                        return left;
                    }
                    throw new BadRequestException("Student has multiple schedule allocations in the same room and time window");
                }));
    }

    private ExamSchedule findScheduleWithTimeslot(Long examScheduleId) {
        return examScheduleRepository.findByIdWithTimeslot(examScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("ExamSchedule", "id", examScheduleId));
    }

    private LocalDateTime toScheduleStart(ExamSchedule schedule) {
        return LocalDateTime.of(schedule.getExamDate(), schedule.getTimeslot().getStartTime());
    }

    private LocalDateTime toScheduleEnd(ExamSchedule schedule) {
        return LocalDateTime.of(schedule.getExamDate(), schedule.getTimeslot().getEndTime());
    }

    private String attendanceKey(Long examScheduleId, Long studentId) {
        return examScheduleId + ":" + studentId;
    }

    private ExamAttendanceStatus normalizeRequestedStatus(ExamAttendanceMarkEntryDTO entry) {
        if (entry.getStatus() == ExamAttendanceStatus.MALPRACTICE) {
            // Legacy payload compatibility: status=MALPRACTICE implies present with malpractice flag.
            return ExamAttendanceStatus.PRESENT;
        }
        return entry.getStatus();
    }

    private boolean normalizeMalpractice(ExamAttendanceMarkEntryDTO entry) {
        return Boolean.TRUE.equals(entry.getMalpractice()) || entry.getStatus() == ExamAttendanceStatus.MALPRACTICE;
    }

    private ExamAttendanceStatus normalizeStoredStatus(ExamAttendanceStatus status) {
        if (status == ExamAttendanceStatus.MALPRACTICE) {
            return ExamAttendanceStatus.PRESENT;
        }
        return status;
    }

    private String buildName(String firstName, String lastName) {
        String left = firstName == null ? "" : firstName.trim();
        String right = lastName == null ? "" : lastName.trim();
        return (left + " " + right).trim();
    }

    private String resolveSeatNumber(String seatNumber, Integer rowNumber, Integer columnNumber) {
        if (seatNumber != null && !seatNumber.isBlank()) {
            return seatNumber;
        }
        if (rowNumber == null || columnNumber == null) {
            return "";
        }
        return "R" + rowNumber + "-C" + columnNumber;
    }
}

