package com.project.edusync.em.model.service;

import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.common.exception.BadRequestException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.em.model.dto.request.SittingPlanRequestDTO;
import com.project.edusync.em.model.dto.request.AutoAllocationRequestDTO;
import com.project.edusync.em.model.dto.response.SittingPlanResponseDTO;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.entity.SittingPlan;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.SittingPlanRepository;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.adm.model.entity.Section;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        Student student = studentRepository.findByUuid(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", dto.getStudentId().toString()));
        Room room = roomRepository.findActiveById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "uuid", dto.getRoomId().toString()));
        if (sittingPlanRepository.findByExamScheduleIdAndStudentId(dto.getExamScheduleId(), student.getId()).isPresent()) {
            throw new BadRequestException("Student already assigned a seat for this exam");
        }
        if (sittingPlanRepository.existsByExamScheduleIdAndRoomIdAndSeatNumber(dto.getExamScheduleId(), room.getId(), dto.getSeatNumber())) {
            throw new BadRequestException("Seat number already taken in this room for this exam");
        }
        long assignedCount = sittingPlanRepository.countByExamScheduleIdAndRoomId(dto.getExamScheduleId(), room.getId());
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

    @Transactional
    public void bulkRemoveSeatAssignments(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("No seat assignment IDs provided");
        }
        List<SittingPlan> plans = sittingPlanRepository.findAllById(ids);
        if (plans.size() != ids.size()) {
            throw new ResourceNotFoundException("SittingPlan", "ids", "Some IDs not found");
        }
        sittingPlanRepository.deleteAllInBatch(plans);
    }

    @Transactional
    public List<SittingPlanResponseDTO> bulkAutoAllocate(AutoAllocationRequestDTO dto) {
        ExamSchedule examSchedule = examScheduleRepository.findById(dto.getExamScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("ExamSchedule", "id", dto.getExamScheduleId()));
        Room room = roomRepository.findActiveById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "uuid", dto.getRoomId().toString()));

        // 1. Fetch all students in the same section or class
        List<Student> students;
        if (examSchedule.getSection() != null) {
            students = studentRepository.findBySectionId(examSchedule.getSection().getId());
        } else if (examSchedule.getAcademicClass() != null) {
            students = studentRepository.findBySection_AcademicClass_Id(examSchedule.getAcademicClass().getId());
        } else {
            throw new BadRequestException("Exam schedule does not have a section or class assigned.");
        }
        if (students == null || students.isEmpty()) {
            throw new BadRequestException("No students found for this class or section.");
        }

        // 2. Filter out already assigned students for this exam
        List<Long> assignedStudentIds = sittingPlanRepository.findByExamScheduleId(dto.getExamScheduleId())
                .stream().map(sp -> sp.getStudent().getId()).collect(Collectors.toList());

        List<Student> unassignedStudents = students.stream()
                .filter(s -> !assignedStudentIds.contains(s.getId()))
                .collect(Collectors.toList());

        // 3. Setup prefix and start number
        String prefix = dto.getSeatPrefix() != null ? dto.getSeatPrefix() : "Seat-";
        int currentNum = dto.getStartNumber() != null ? dto.getStartNumber() : 1;

        // 4. Room capacity check
        long currentlyAssignedInRoom = sittingPlanRepository.countByExamScheduleIdAndRoomId(dto.getExamScheduleId(), room.getId());
        int roomCapacity = room.getCapacity() != null ? room.getCapacity() : Integer.MAX_VALUE;

        List<SittingPlan> newPlans = new ArrayList<>();
        for (Student s : unassignedStudents) {
            if (currentlyAssignedInRoom >= roomCapacity) break;

            // Generate seat number
            String seatNumber;
            boolean exists;
            do {
                seatNumber = prefix + currentNum++;
                exists = sittingPlanRepository.existsByExamScheduleIdAndRoomIdAndSeatNumber(dto.getExamScheduleId(), room.getId(), seatNumber);
            } while (exists);

            SittingPlan plan = SittingPlan.builder()
                    .examSchedule(examSchedule)
                    .student(s)
                    .room(room)
                    .seatNumber(seatNumber)
                    .build();
            newPlans.add(plan);
            currentlyAssignedInRoom++;
        }

        return sittingPlanRepository.saveAll(newPlans).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
