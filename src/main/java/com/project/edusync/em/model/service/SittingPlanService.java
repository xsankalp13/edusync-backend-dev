package com.project.edusync.em.model.service;

import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.common.exception.BadRequestException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.em.model.dto.request.SittingPlanRequestDTO;
import com.project.edusync.em.model.dto.response.SittingPlanResponseDTO;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.entity.SittingPlan;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.SittingPlanRepository;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SittingPlanService {
    private final SittingPlanRepository sittingPlanRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public SittingPlanResponseDTO assignSeat(SittingPlanRequestDTO dto) {
        if (dto.getSeatNumber() == null || dto.getSeatNumber().trim().isEmpty()) {
            throw new BadRequestException("Seat number cannot be null or empty");
        }
        ExamSchedule examSchedule = examScheduleRepository.findById(dto.getExamScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("ExamSchedule", "id", dto.getExamScheduleId()));
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", dto.getStudentId()));
        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", dto.getRoomId()));
        if (sittingPlanRepository.findByExamScheduleIdAndStudentId(dto.getExamScheduleId(), dto.getStudentId()).isPresent()) {
            throw new BadRequestException("Student already assigned a seat for this exam");
        }
        if (sittingPlanRepository.existsByExamScheduleIdAndRoomIdAndSeatNumber(dto.getExamScheduleId(), dto.getRoomId(), dto.getSeatNumber())) {
            throw new BadRequestException("Seat number already taken in this room for this exam");
        }
        long assignedCount = sittingPlanRepository.countByExamScheduleIdAndRoomId(dto.getExamScheduleId(), dto.getRoomId());
        Integer roomCapacity = room.getCapacity();
        if (roomCapacity != null && assignedCount >= roomCapacity) {
            throw new BadRequestException("Room capacity exceeded");
        }
        SittingPlan sittingPlan = SittingPlan.builder()
                .examSchedule(examSchedule)
                .student(student)
                .room(room)
                .seatNumber(dto.getSeatNumber())
                .build();
        SittingPlan saved = sittingPlanRepository.save(sittingPlan);
        return toResponse(saved);
    }

    public List<SittingPlanResponseDTO> getSittingPlanByExam(Long examScheduleId) {
        List<SittingPlan> sittingPlans = sittingPlanRepository.findByExamScheduleId(examScheduleId);
        return sittingPlans.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SittingPlanResponseDTO> getSittingPlanByRoom(Long roomId) {
        List<SittingPlan> sittingPlans = sittingPlanRepository.findByRoomId(roomId);
        return sittingPlans.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeSeatAssignment(Long id) {
        SittingPlan sittingPlan = sittingPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SittingPlan", "id", id));
        sittingPlanRepository.delete(sittingPlan);
    }

    private SittingPlanResponseDTO toResponse(SittingPlan sp) {
        SittingPlanResponseDTO dto = new SittingPlanResponseDTO();
        dto.setId(sp.getId());
        // Build full name from UserProfile fields
        String firstName = sp.getStudent().getUserProfile().getFirstName();
        String middleName = sp.getStudent().getUserProfile().getMiddleName();
        String lastName = sp.getStudent().getUserProfile().getLastName();
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) fullName.append(firstName).append(" ");
        if (middleName != null && !middleName.isBlank()) fullName.append(middleName).append(" ");
        if (lastName != null) fullName.append(lastName);
        dto.setStudentName(fullName.toString().trim());
        dto.setRoomName(sp.getRoom().getName());
        dto.setSeatNumber(sp.getSeatNumber());
        dto.setExamScheduleId(sp.getExamSchedule().getId());
        return dto;
    }
}
