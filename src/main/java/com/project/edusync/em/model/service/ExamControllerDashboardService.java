package com.project.edusync.em.model.service;

import com.project.edusync.em.model.dto.response.ExamControllerClassViewResponseDTO;
import com.project.edusync.em.model.dto.response.ExamControllerDashboardResponseDTO;
import com.project.edusync.em.model.dto.response.ExamControllerRoomViewResponseDTO;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.enums.RoomAttendanceProgressStatus;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.SeatAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ExamControllerDashboardService {

    private final SeatAllocationRepository seatAllocationRepository;
    private final ExamScheduleRepository examScheduleRepository;

    public ExamControllerDashboardResponseDTO getDashboard(Long examId) {
        List<ExamControllerDashboardResponseDTO.RoomSummary> roomSummaries = seatAllocationRepository
            .findExamControllerRoomSummariesByExamId(examId)
            .stream()
            .map(this::toRoomSummary)
            .toList();

        return ExamControllerDashboardResponseDTO.builder()
            .examId(examId)
            .rooms(roomSummaries)
            .timer(resolveTimer(examId))
            .build();
    }

    public ExamControllerClassViewResponseDTO getClassView(Long examId) {
        Map<String, Map<Long, MutableClassStudent>> classMap = new LinkedHashMap<>();

        for (SeatAllocationRepository.ExamControllerStudentSeatProjection row : seatAllocationRepository.findExamControllerStudentRowsByExamId(examId)) {
            Map<Long, MutableClassStudent> studentMap =
                classMap.computeIfAbsent(safe(row.getClassName()), ignored -> new LinkedHashMap<>());

            MutableClassStudent student = studentMap.computeIfAbsent(
                row.getStudentId(),
                ignored -> new MutableClassStudent(
                    row.getStudentId(),
                    buildName(row.getFirstName(), row.getLastName()),
                    row.getRollNo(),
                    new ArrayList<>()
                )
            );

            student.rooms.add(ExamControllerClassViewResponseDTO.RoomAssignmentNode.builder()
                .roomId(row.getRoomId())
                .roomName(row.getRoomName())
                .examScheduleId(row.getExamScheduleId())
                .subjectName(row.getSubjectName())
                .seatNumber(resolveSeatNumber(row.getSeatNumber(), row.getRowNumber(), row.getColumnNumber()))
                .attendanceStatus(resolveAttendanceStatus(row.getAttendanceStatus()))
                .entryAllowed(Boolean.TRUE.equals(row.getEntryAllowed()))
                .build());
        }

        List<ExamControllerClassViewResponseDTO.ClassNode> classes = classMap.entrySet().stream()
            .map(classEntry -> ExamControllerClassViewResponseDTO.ClassNode.builder()
                .className(classEntry.getKey())
                .students(classEntry.getValue().values().stream().map(value ->
                    ExamControllerClassViewResponseDTO.StudentNode.builder()
                        .studentId(value.studentId)
                        .studentName(value.studentName)
                        .rollNo(value.rollNo)
                        .rooms(value.rooms)
                        .build()).toList())
                .build())
            .toList();

        return ExamControllerClassViewResponseDTO.builder()
            .examId(examId)
            .classes(classes)
            .build();
    }

    public ExamControllerRoomViewResponseDTO getRoomView(Long examId) {
        Map<Long, MutableRoomNode> roomMap = new LinkedHashMap<>();

        for (SeatAllocationRepository.ExamControllerStudentSeatProjection row : seatAllocationRepository.findExamControllerStudentRowsByExamId(examId)) {
            MutableRoomNode room = roomMap.computeIfAbsent(
                row.getRoomId(),
                ignored -> new MutableRoomNode(
                    row.getRoomId(),
                    row.getRoomName(),
                    new ArrayList<>()
                )
            );

            room.students.add(ExamControllerRoomViewResponseDTO.StudentNode.builder()
                .studentId(row.getStudentId())
                .studentName(buildName(row.getFirstName(), row.getLastName()))
                .rollNo(row.getRollNo())
                .className(row.getClassName())
                .examScheduleId(row.getExamScheduleId())
                .subjectName(row.getSubjectName())
                .seatNumber(resolveSeatNumber(row.getSeatNumber(), row.getRowNumber(), row.getColumnNumber()))
                .attendanceStatus(resolveAttendanceStatus(row.getAttendanceStatus()))
                .entryAllowed(Boolean.TRUE.equals(row.getEntryAllowed()))
                .build());
        }

        return ExamControllerRoomViewResponseDTO.builder()
            .examId(examId)
            .rooms(roomMap.values().stream().map(value ->
                ExamControllerRoomViewResponseDTO.RoomNode.builder()
                    .roomId(value.roomId)
                    .roomName(value.roomName)
                    .students(value.students)
                    .build()).toList())
            .build();
    }

    private ExamControllerDashboardResponseDTO.RoomSummary toRoomSummary(SeatAllocationRepository.ExamControllerRoomSummaryProjection projection) {
        long allocated = projection.getAllocatedCount();
        long marked = projection.getMarkedCount();
        return ExamControllerDashboardResponseDTO.RoomSummary.builder()
            .roomId(projection.getRoomId())
            .roomName(projection.getRoomName())
            .allocatedStudents(allocated)
            .markedStudents(marked)
            .attendanceStatus(resolveRoomStatus(allocated, marked))
            .build();
    }

    private RoomAttendanceProgressStatus resolveRoomStatus(long allocated, long marked) {
        if (allocated == 0 || marked == 0) {
            return RoomAttendanceProgressStatus.NOT_STARTED;
        }
        if (marked < allocated) {
            return RoomAttendanceProgressStatus.IN_PROGRESS;
        }
        return RoomAttendanceProgressStatus.COMPLETED;
    }

    private ExamControllerDashboardResponseDTO.TimerInfo resolveTimer(Long examId) {
        List<ExamSchedule> schedules = examScheduleRepository.findByExamIdWithDetails(examId);
        if (schedules.isEmpty()) {
            return ExamControllerDashboardResponseDTO.TimerInfo.builder().remainingSeconds(0).build();
        }

        List<Window> windows = schedules.stream()
            .map(s -> new Window(
                LocalDateTime.of(s.getExamDate(), s.getTimeslot().getStartTime()),
                LocalDateTime.of(s.getExamDate(), s.getTimeslot().getEndTime())
            ))
            .sorted(Comparator.comparing(Window::start))
            .toList();

        LocalDateTime now = LocalDateTime.now();
        Window target = windows.get(windows.size() - 1);
        for (Window window : windows) {
            if (!now.isAfter(window.end)) {
                target = window;
                break;
            }
        }

        long remaining = now.isBefore(target.end) ? Duration.between(now, target.end).getSeconds() : 0;

        return ExamControllerDashboardResponseDTO.TimerInfo.builder()
            .startTime(target.start)
            .endTime(target.end)
            .remainingSeconds(Math.max(remaining, 0))
            .build();
    }

    private String resolveAttendanceStatus(Enum<?> status) {
        return status == null ? "NOT_MARKED" : status.name();
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

    private String buildName(String firstName, String lastName) {
        return (safe(firstName) + " " + safe(lastName)).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record Window(LocalDateTime start, LocalDateTime end) {
    }

    private record MutableClassStudent(Long studentId,
                                       String studentName,
                                       Integer rollNo,
                                       List<ExamControllerClassViewResponseDTO.RoomAssignmentNode> rooms) {
    }

    private record MutableRoomNode(Long roomId,
                                   String roomName,
                                   List<ExamControllerRoomViewResponseDTO.StudentNode> students) {
    }
}

