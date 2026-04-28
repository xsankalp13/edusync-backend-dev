package com.project.edusync.em.model.service;

import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.em.model.dto.request.ExamEntryDecisionRequestDTO;
import com.project.edusync.em.model.dto.response.ExamEntryDecisionResponseDTO;
import com.project.edusync.em.model.entity.ExamEntryDecision;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.repository.ExamEntryDecisionRepository;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExamEntryDecisionService {

    private final ExamScheduleRepository examScheduleRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final ExamAttendanceService examAttendanceService;
    private final ExamEntryDecisionRepository examEntryDecisionRepository;

    @Transactional
    public ExamEntryDecisionResponseDTO saveDecision(ExamEntryDecisionRequestDTO requestDTO) {
        ExamSchedule schedule = examScheduleRepository.findById(requestDTO.getExamScheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("ExamSchedule", "id", requestDTO.getExamScheduleId()));

        Student student = studentRepository.findById(requestDTO.getStudentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", requestDTO.getStudentId()));

        Long markerStaffId = examAttendanceService.resolveCurrentStaffIdForPrivilegedAction();
        Staff marker = staffRepository.findById(markerStaffId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", markerStaffId));

        ExamEntryDecision decision = examEntryDecisionRepository
            .findByExamScheduleIdAndStudentId(requestDTO.getExamScheduleId(), requestDTO.getStudentId())
            .orElseGet(ExamEntryDecision::new);

        decision.setExamSchedule(schedule);
        decision.setStudent(student);
        decision.setAllowed(Boolean.TRUE.equals(requestDTO.getAllowed()));
        decision.setReason(requestDTO.getReason());
        decision.setDecidedBy(marker);

        ExamEntryDecision saved = examEntryDecisionRepository.save(decision);

        return ExamEntryDecisionResponseDTO.builder()
            .examScheduleId(saved.getExamSchedule().getId())
            .studentId(saved.getStudent().getId())
            .allowed(saved.isAllowed())
            .reason(saved.getReason())
            .decidedByStaffId(saved.getDecidedBy().getId())
            .decidedAt(saved.getDecidedAt())
            .build();
    }
}

