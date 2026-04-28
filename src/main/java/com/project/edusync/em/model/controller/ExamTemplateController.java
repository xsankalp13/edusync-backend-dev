package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.ExamTemplateRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.EvaluationStructureResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamTemplateResponseDTO;
import com.project.edusync.em.model.service.ExamTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/examination/templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
public class ExamTemplateController {

    private final ExamTemplateService examTemplateService;

    @PostMapping
    public ResponseEntity<ExamTemplateResponseDTO> createTemplate(@Valid @RequestBody ExamTemplateRequestDTO requestDTO) {
        return new ResponseEntity<>(examTemplateService.createTemplate(requestDTO), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ExamTemplateResponseDTO>> getAllTemplates() {
        return ResponseEntity.ok(examTemplateService.getAllTemplates());
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<ExamTemplateResponseDTO> getTemplateById(@PathVariable UUID templateId) {
        return ResponseEntity.ok(examTemplateService.getTemplateById(templateId));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<ExamTemplateResponseDTO> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody ExamTemplateRequestDTO requestDTO) {
        return ResponseEntity.ok(examTemplateService.updateTemplate(templateId, requestDTO));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId) {
        examTemplateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{templateId}/preview")
    public ResponseEntity<EvaluationStructureResponseDTO> getTemplatePreview(@PathVariable UUID templateId) {
        return ResponseEntity.ok(examTemplateService.getTemplatePreview(templateId));
    }
}

