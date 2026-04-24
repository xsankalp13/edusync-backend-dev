package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.GradeSystemRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.GradeSystemResponseDTO;
import com.project.edusync.em.model.service.GradeSystemService;
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
@RequestMapping("${api.url}/auth/examination/grade-systems")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
@Tag(name = "Grade Systems", description = "APIs for managing grading logic (e.g., CBSE, ICSE)")
public class GradeSystemController {

    private final GradeSystemService gradeSystemService;

    @PostMapping
    @Operation(summary = "Create a new Grade System with its rules")
    public ResponseEntity<GradeSystemResponseDTO> createGradeSystem(
            @Valid @RequestBody GradeSystemRequestDTO requestDTO) {
        return new ResponseEntity<>(gradeSystemService.createGradeSystem(requestDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all active Grade Systems")
    public ResponseEntity<List<GradeSystemResponseDTO>> getAllActiveGradeSystems() {
        return ResponseEntity.ok(gradeSystemService.getAllActiveGradeSystems());
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get a specific Grade System by UUID")
    public ResponseEntity<GradeSystemResponseDTO> getGradeSystemByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(gradeSystemService.getGradeSystemByUuid(uuid));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update an existing Grade System and its rules")
    public ResponseEntity<GradeSystemResponseDTO> updateGradeSystem(
            @PathVariable UUID uuid,
            @Valid @RequestBody GradeSystemRequestDTO requestDTO) {
        return ResponseEntity.ok(gradeSystemService.updateGradeSystem(uuid, requestDTO));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Soft-delete a Grade System")
    public ResponseEntity<Void> deleteGradeSystem(@PathVariable UUID uuid) {
        gradeSystemService.deleteGradeSystem(uuid);
        return ResponseEntity.noContent().build();
    }
}