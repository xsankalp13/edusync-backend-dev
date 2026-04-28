package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.ExamScheduleRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.EvaluationStructureResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamScheduleResponseDTO;
import com.project.edusync.em.model.service.ExamScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/examination")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    /**
     * Create a new schedule entry for a specific exam.
     * POST /api/v1/examination/exams/{examUuid}/schedules
     */
    @PostMapping("/exams/{examUuid}/schedules")
    @PreAuthorize("@examControllerAccess.canAccessExamUuid(#examUuid)")
    public ResponseEntity<ExamScheduleResponseDTO> createSchedule(
            @PathVariable UUID examUuid,
            @Valid @RequestBody ExamScheduleRequestDTO requestDTO) {
        ExamScheduleResponseDTO response = examScheduleService.createSchedule(examUuid, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all schedule entries for a specific exam.
     * GET /api/v1/examination/exams/{examUuid}/schedules
     */
    @GetMapping("/exams/{examUuid}/schedules")
    @PreAuthorize("@examControllerAccess.canAccessExamUuid(#examUuid)")
    public ResponseEntity<List<ExamScheduleResponseDTO>> getSchedulesByExam(@PathVariable UUID examUuid) {
        return ResponseEntity.ok(examScheduleService.getSchedulesByExam(examUuid));
    }

    /**
     * Get a specific schedule entry by its ID.
     * GET /api/v1/examination/schedules/{scheduleId}
     */
    @GetMapping("/schedules/{scheduleId}")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#scheduleId)")
    public ResponseEntity<ExamScheduleResponseDTO> getScheduleById(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(examScheduleService.getScheduleById(scheduleId));
    }

    @GetMapping("/schedules/{scheduleId}/evaluation-structure")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#scheduleId)")
    public ResponseEntity<EvaluationStructureResponseDTO> getEvaluationStructure(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(examScheduleService.getEvaluationStructure(scheduleId));
    }

    /**
     * Update a specific schedule entry.
     * PUT /api/v1/examination/schedules/{scheduleId}
     */
    @PutMapping("/schedules/{scheduleId}")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#scheduleId)")
    public ResponseEntity<ExamScheduleResponseDTO> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ExamScheduleRequestDTO requestDTO) {
        return ResponseEntity.ok(examScheduleService.updateSchedule(scheduleId, requestDTO));
    }

    /**
     * Delete a schedule entry.
     * DELETE /api/v1/examination/schedules/{scheduleId}
     */
    @DeleteMapping("/schedules/{scheduleId}")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#scheduleId)")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId) {
        examScheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }
}
