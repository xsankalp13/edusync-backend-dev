package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.EvaluationAssignmentCreateRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.EvaluationAssignmentResponseDTO;
import com.project.edusync.em.model.service.AnswerEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/examination/evaluation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_SUPER_ADMIN','ROLE_EXAM_CONTROLLER')")
public class AnswerEvaluationAdminController {

    private final AnswerEvaluationService answerEvaluationService;

    @PostMapping("/assign")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#requestDTO.examScheduleId)")
    public ResponseEntity<EvaluationAssignmentResponseDTO> assignTeacher(
            @Valid @RequestBody EvaluationAssignmentCreateRequestDTO requestDTO) {
        return new ResponseEntity<>(answerEvaluationService.assignTeacher(requestDTO), HttpStatus.CREATED);
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<EvaluationAssignmentResponseDTO>> getAssignments(
            @RequestParam(required = false) UUID teacherId) {
        return ResponseEntity.ok(answerEvaluationService.getAssignmentsForAdmin(teacherId));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("@examControllerAccess.canAccessEvaluationAssignment(#assignmentId)")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long assignmentId) {
        answerEvaluationService.deleteAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }
}

