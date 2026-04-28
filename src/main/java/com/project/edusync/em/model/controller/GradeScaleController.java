package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.GradeScaleRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.GradeScaleResponseDTO;
import com.project.edusync.em.model.service.GradeScaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Grade Scales", description = "APIs for managing individual grade rules within a system")
public class GradeScaleController {

    private final GradeScaleService gradeScaleService;

    /**
     * Add a new scale rule to a specific Grade System.
     * POST /api/v1/examination/grade-systems/{systemUuid}/scales
     */
    @PostMapping("/grade-systems/{systemUuid}/scales")
    @Operation(summary = "Add a single scale rule to a grade system")
    public ResponseEntity<GradeScaleResponseDTO> addScaleToSystem(
            @PathVariable UUID systemUuid,
            @Valid @RequestBody GradeScaleRequestDTO requestDTO) {
        GradeScaleResponseDTO response = gradeScaleService.addScaleToSystem(systemUuid, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all scales for a specific Grade System.
     * GET /api/v1/examination/grade-systems/{systemUuid}/scales
     */
    @GetMapping("/grade-systems/{systemUuid}/scales")
    @Operation(summary = "Get all scales for a specific grade system")
    public ResponseEntity<List<GradeScaleResponseDTO>> getScalesBySystem(@PathVariable UUID systemUuid) {
        return ResponseEntity.ok(gradeScaleService.getScalesBySystemUuid(systemUuid));
    }

    /**
     * Get a specific scale by its ID.
     * GET /api/v1/examination/grade-scales/{scaleId}
     */
    @GetMapping("/grade-scales/{scaleId}")
    @Operation(summary = "Get a specific grade scale rule")
    public ResponseEntity<GradeScaleResponseDTO> getGradeScaleById(@PathVariable Long scaleId) {
        return ResponseEntity.ok(gradeScaleService.getGradeScaleById(scaleId));
    }

    /**
     * Update a specific scale rule.
     * PUT /api/v1/examination/grade-scales/{scaleId}
     */
    @PutMapping("/grade-scales/{scaleId}")
    @Operation(summary = "Update a specific grade scale rule")
    public ResponseEntity<GradeScaleResponseDTO> updateGradeScale(
            @PathVariable Long scaleId,
            @Valid @RequestBody GradeScaleRequestDTO requestDTO) {
        return ResponseEntity.ok(gradeScaleService.updateGradeScale(scaleId, requestDTO));
    }

    /**
     * Delete a specific scale rule.
     * DELETE /api/v1/examination/grade-scales/{scaleId}
     */
    @DeleteMapping("/grade-scales/{scaleId}")
    @Operation(summary = "Delete a specific grade scale rule")
    public ResponseEntity<Void> deleteGradeScale(@PathVariable Long scaleId) {
        gradeScaleService.deleteGradeScale(scaleId);
        return ResponseEntity.noContent().build();
    }
}