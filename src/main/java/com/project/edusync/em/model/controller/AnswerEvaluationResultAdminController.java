package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.ResponseDTO.AdminResultReviewResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.EvaluationResultResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ClassResultSummaryResponseDTO;
import com.project.edusync.em.model.enums.EvaluationResultStatus;
import com.project.edusync.em.model.service.AnswerEvaluationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/results")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_SUPER_ADMIN','ROLE_EXAM_CONTROLLER')")
public class AnswerEvaluationResultAdminController {

    private final AnswerEvaluationService answerEvaluationService;

    @GetMapping
    public ResponseEntity<List<AdminResultReviewResponseDTO>> getResults(
            @RequestParam(required = false) EvaluationResultStatus status) {
        return ResponseEntity.ok(answerEvaluationService.getResultsForAdmin(status));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@examControllerAccess.canAccessEvaluationResult(#resultId)")
    public ResponseEntity<EvaluationResultResponseDTO> approveResult(@PathVariable("id") Long resultId) {
        return ResponseEntity.ok(answerEvaluationService.approveResult(resultId));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@examControllerAccess.canAccessEvaluationResult(#resultId)")
    public ResponseEntity<EvaluationResultResponseDTO> rejectResult(@PathVariable("id") Long resultId) {
        return ResponseEntity.ok(answerEvaluationService.rejectResult(resultId));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@examControllerAccess.canAccessEvaluationResult(#resultId)")
    public ResponseEntity<EvaluationResultResponseDTO> publishResult(@PathVariable("id") Long resultId) {
        return ResponseEntity.ok(answerEvaluationService.publishResult(resultId));
    }

    @PostMapping("/publish-bulk")
    @PreAuthorize("@examControllerAccess.canAccessEvaluationResults(#resultIds)")
    public ResponseEntity<Map<String, Integer>> publishBulk(@org.springframework.web.bind.annotation.RequestBody List<Long> resultIds) {
        int count = answerEvaluationService.publishResultsBulk(resultIds);
        return ResponseEntity.ok(Map.of("publishedCount", count));
    }

    @GetMapping("/summary")
    @PreAuthorize("@examControllerAccess.canAccessExamUuid(#examId)")
    public ResponseEntity<ClassResultSummaryResponseDTO> getSummary(
            @RequestParam("classId") UUID classId,
            @RequestParam("examId") UUID examId) {
        return ResponseEntity.ok(answerEvaluationService.getClassResultSummary(classId, examId));
    }

    @PostMapping("/approve-class")
    @PreAuthorize("@examControllerAccess.canAccessExamUuid(#examId)")
    public ResponseEntity<Map<String, Integer>> approveClass(
            @RequestParam("classId") UUID classId,
            @RequestParam("examId") UUID examId) {
        int count = answerEvaluationService.approveClassResults(classId, examId);
        return ResponseEntity.ok(Map.of("approvedCount", count));
    }

    @PostMapping("/publish-class")
    @PreAuthorize("@examControllerAccess.canAccessExamUuid(#examId)")
    public ResponseEntity<Map<String, Integer>> publishClass(
            @RequestParam("classId") UUID classId,
            @RequestParam("examId") UUID examId) {
        int count = answerEvaluationService.publishClassResults(classId, examId);
        return ResponseEntity.ok(Map.of("publishedCount", count));
    }

    @PostMapping("/mark-absent")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#scheduleId)")
    public ResponseEntity<Void> markAbsent(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("studentId") Long studentId,
            @RequestParam("isAbsent") boolean isAbsent) {
        answerEvaluationService.markStudentAbsent(scheduleId, studentId, isAbsent);
        return ResponseEntity.ok().build();
    }
}

