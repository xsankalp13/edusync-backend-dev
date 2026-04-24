package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.PaperQuestionMapRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.PaperQuestionMapResponseDTO;
import com.project.edusync.em.model.service.serviceImpl.PaperQuestionMapServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.url}/auth/examination")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
@Tag(name = "Paper-Question Mappings", description = "APIs for fine-tuning questions within a generated paper")
public class PaperQuestionMapController {

    private final PaperQuestionMapServiceImpl paperQuestionMapService;

    /**
     * Add a new question to an existing paper.
     * POST /api/v1/examination/papers/{paperId}/questions
     */
    @PostMapping("/papers/{paperId}/questions")
    @Operation(summary = "Add a single question to a paper")
    public ResponseEntity<PaperQuestionMapResponseDTO> addMapping(
            @PathVariable Long paperId,
            @Valid @RequestBody PaperQuestionMapRequestDTO requestDTO) {
        PaperQuestionMapResponseDTO response = paperQuestionMapService.addMapping(paperId, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update details of a specific mapping (e.g., change its number or marks).
     * PUT /api/v1/examination/paper-mappings/{mappingId}
     */
    @PutMapping("/paper-mappings/{mappingId}")
    @Operation(summary = "Update a specific question mapping")
    public ResponseEntity<PaperQuestionMapResponseDTO> updateMapping(
            @PathVariable Long mappingId,
            @Valid @RequestBody PaperQuestionMapRequestDTO requestDTO) {
        PaperQuestionMapResponseDTO response = paperQuestionMapService.updateMapping(mappingId, requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a question from a paper.
     * DELETE /api/v1/examination/paper-mappings/{mappingId}
     */
    @DeleteMapping("/paper-mappings/{mappingId}")
    @Operation(summary = "Remove a question from a paper")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long mappingId) {
        paperQuestionMapService.deleteMapping(mappingId);
        return ResponseEntity.noContent().build();
    }
}