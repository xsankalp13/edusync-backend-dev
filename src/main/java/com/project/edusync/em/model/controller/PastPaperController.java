package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.PastPaperRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.PastPaperResponseDTO;
import com.project.edusync.em.model.service.PastPaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/examination/past-papers")
@RequiredArgsConstructor
@Tag(name = "Past Papers", description = "APIs for managing uploaded past examination papers")
public class PastPaperController {

    private final PastPaperService pastPaperService;

    /**
     * Upload a new past paper.
     * Expects multipart/form-data with 'metadata' (JSON) and 'file' (binary).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Upload a new past paper PDF")
    public ResponseEntity<PastPaperResponseDTO> uploadPastPaper(
            @RequestPart("metadata") @Valid PastPaperRequestDTO requestDTO,
            @RequestPart("file") MultipartFile file) {
        return new ResponseEntity<>(pastPaperService.uploadPastPaper(requestDTO, file), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN','SCHOOL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get all past papers with optional filters")
    public ResponseEntity<List<PastPaperResponseDTO>> getAllPastPapers(
            @Parameter(description = "Filter by Class UUID") @RequestParam(required = false) UUID classId,
            @Parameter(description = "Filter by Subject UUID") @RequestParam(required = false) UUID subjectId,
            @Parameter(description = "Filter by Exam Year") @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(pastPaperService.getAllPastPapers(classId, subjectId, year));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN','SCHOOL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get a specific past paper by UUID")
    public ResponseEntity<PastPaperResponseDTO> getPastPaperByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(pastPaperService.getPastPaperByUuid(uuid));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Delete a past paper")
    public ResponseEntity<Void> deletePastPaper(@PathVariable UUID uuid) {
        pastPaperService.deletePastPaper(uuid);
        return ResponseEntity.noContent().build();
    }
}