package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.BulkMarkRequestDTO;
import com.project.edusync.em.model.dto.RequestDTO.StudentMarkRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.StudentMarkResponseDTO;
import com.project.edusync.em.model.service.StudentMarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/examination")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER','TEACHER')")
@Tag(name = "Student Marks", description = "APIs for recording and managing student exam results")
public class StudentMarkController {

    private final StudentMarkService studentMarkService;

    /**
     * Bulk record or update marks for a specific exam schedule.
     * Ideal for teachers submitting grades for an entire class.
     */
    @PostMapping("/schedules/{scheduleId}/marks")
    @Operation(summary = "Submit marks for multiple students for a schedule (Bulk Upsert)")
    public ResponseEntity<List<StudentMarkResponseDTO>> recordBulkMarks(
            @PathVariable Long scheduleId,
            @Valid @RequestBody BulkMarkRequestDTO bulkRequest) {
        return ResponseEntity.ok(studentMarkService.recordBulkMarks(scheduleId, bulkRequest));
    }

    /**
     * Get all marks entered for a specific schedule.
     */
    @GetMapping("/schedules/{scheduleId}/marks")
    @Operation(summary = "Get all recorded marks for a specific exam schedule")
    public ResponseEntity<List<StudentMarkResponseDTO>> getMarksBySchedule(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(studentMarkService.getMarksBySchedule(scheduleId));
    }

    /**
     * Update a single mark entry by its UUID.
     * Useful for quick corrections without resubmitting the whole list.
     */
    @PutMapping("/marks/{markUuid}")
    @Operation(summary = "Update a specific mark entry")
    public ResponseEntity<StudentMarkResponseDTO> updateMark(
            @PathVariable UUID markUuid,
            @Valid @RequestBody StudentMarkRequestDTO requestDTO) {
        return ResponseEntity.ok(studentMarkService.updateMark(markUuid, requestDTO));
    }
}