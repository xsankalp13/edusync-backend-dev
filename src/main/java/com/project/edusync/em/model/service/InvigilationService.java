package com.project.edusync.em.model.service;

import com.project.edusync.common.exception.BadRequestException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.em.model.dto.request.InvigilationRequestDTO;
import com.project.edusync.em.model.dto.request.PoolBulkAssignRequestDTO;
import com.project.edusync.em.model.dto.response.InvigilationResponseDTO;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.entity.Invigilation;
import com.project.edusync.em.model.enums.InvigilationRole;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.InvigilationRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvigilationService {
    private final InvigilationRepository invigilationRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StaffRepository staffRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public InvigilationResponseDTO assignInvigilator(InvigilationRequestDTO dto) {
        if (dto.getRole() == null) {
            throw new BadRequestException("Role cannot be null");
        }
        ExamSchedule examSchedule = examScheduleRepository.findById(dto.getExamScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("ExamSchedule", "id", dto.getExamScheduleId()));
        Staff staff = staffRepository.findByUuid(dto.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", dto.getStaffId().toString()));
        Room room = roomRepository.findByUuid(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "uuid", dto.getRoomId().toString()));

        if (invigilationRepository.existsByExamScheduleIdAndStaffId(dto.getExamScheduleId(), staff.getId())) {
            throw new BadRequestException("Staff already assigned to this exam");
        }
        if (dto.getRole() == InvigilationRole.PRIMARY && invigilationRepository.existsByExamScheduleIdAndRole(dto.getExamScheduleId(), InvigilationRole.PRIMARY)) {
            throw new BadRequestException("Only one PRIMARY invigilator allowed per exam");
        }
        Long timeslotId = examSchedule.getTimeslot().getId();
        boolean timeslotConflict = !invigilationRepository.findByStaffIdAndExamSchedule_TimeslotId(staff.getId(), timeslotId).isEmpty();
        if (timeslotConflict) {
            throw new BadRequestException("Staff is already assigned to another exam in the same timeslot");
        }
        Invigilation invigilation = Invigilation.builder()
                .examSchedule(examSchedule)
                .staff(staff)
                .room(room)
                .role(dto.getRole())
                .build();
        Invigilation saved = invigilationRepository.save(invigilation);
        return toResponse(saved);
    }

    @Transactional
    public void bulkAssignPool(PoolBulkAssignRequestDTO dto) {
        List<ExamSchedule> schedules = examScheduleRepository.findByExamUuid(dto.getExamUuid());
        if (schedules.isEmpty()) return;

        List<Long> scheduleIds = schedules.stream().map(ExamSchedule::getId).collect(Collectors.toList());
        invigilationRepository.deleteAllByExamScheduleIdIn(scheduleIds);
        invigilationRepository.flush();

        if (dto.getPoolStaffUuids() == null || dto.getPoolStaffUuids().isEmpty()) return;
        List<Staff> pool = staffRepository.findByUuidIn(dto.getPoolStaffUuids());
        if (pool.isEmpty()) return;

        if (dto.getSelectedRoomUuids() == null || dto.getSelectedRoomUuids().isEmpty()) return;
        List<Room> rooms = roomRepository.findByUuidIn(dto.getSelectedRoomUuids());
        if (rooms.isEmpty()) return;

        // Group by timeslot to avoid conflict in the same timeslot
        java.util.Map<Long, List<ExamSchedule>> schedulesByTimeslot = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getTimeslot().getId()));

        int staffIndex = 0;

        for (List<ExamSchedule> slotSchedules : schedulesByTimeslot.values()) {
            for (ExamSchedule schedule : slotSchedules) {
                int localStaffIndex = staffIndex;
                for (Room room : rooms) {
                    if (localStaffIndex - staffIndex >= pool.size()) {
                        break; // exhausted pool for this timeslot
                    }
                    Staff staff = pool.get(localStaffIndex % pool.size());
                    Invigilation inv = Invigilation.builder()
                            .examSchedule(schedule)
                            .staff(staff)
                            .room(room)
                            .role(InvigilationRole.PRIMARY)
                            .build();
                    invigilationRepository.save(inv);
                    localStaffIndex++;
                }
            }
            // Advance the starting staff index for the next timeslot (Option C: cycle pool)
            staffIndex = (staffIndex + rooms.size()) % pool.size();
        }
    }

    public List<InvigilationResponseDTO> getInvigilatorsByExam(Long examScheduleId) {
        return invigilationRepository.findByExamScheduleId(examScheduleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<InvigilationResponseDTO> getInvigilationsByStaff(Long staffId) {
        return invigilationRepository.findByStaffId(staffId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeInvigilator(Long id) {
        Invigilation invigilation = invigilationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invigilation", "id", id));
        invigilationRepository.delete(invigilation);
    }

    private InvigilationResponseDTO toResponse(Invigilation inv) {
        InvigilationResponseDTO dto = new InvigilationResponseDTO();
        dto.setId(inv.getId());
        // Build staff name from UserProfile fields
        String firstName = inv.getStaff().getUserProfile().getFirstName();
        String middleName = inv.getStaff().getUserProfile().getMiddleName();
        String lastName = inv.getStaff().getUserProfile().getLastName();
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) fullName.append(firstName).append(" ");
        if (middleName != null && !middleName.isBlank()) fullName.append(middleName).append(" ");
        if (lastName != null) fullName.append(lastName);
        dto.setStaffName(fullName.toString().trim());
        dto.setRole(inv.getRole());
        dto.setExamScheduleId(inv.getExamSchedule().getId());
        if (inv.getRoom() != null) {
            dto.setRoomUuid(inv.getRoom().getUuid().toString());
            dto.setRoomName(inv.getRoom().getName());
        }
        return dto;
    }
}

