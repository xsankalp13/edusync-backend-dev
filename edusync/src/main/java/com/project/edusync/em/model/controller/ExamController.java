package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.ExamRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamResponseDTO;
import com.project.edusync.em.model.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/examination/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<ExamResponseDTO> createExam(@Valid @RequestBody ExamRequestDTO requestDTO) {
        ExamResponseDTO response = examService.createExam(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ExamResponseDTO> getExamByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(examService.getExamByUuid(uuid));
    }

    @GetMapping
    public ResponseEntity<List<ExamResponseDTO>> getAllExams() {
        return ResponseEntity.ok(examService.getAllExams());
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ExamResponseDTO> updateExam(@PathVariable UUID uuid, @Valid @RequestBody ExamRequestDTO requestDTO) {
        return ResponseEntity.ok(examService.updateExam(uuid, requestDTO));
    }

    @PatchMapping("/{uuid}/publish")
    public ResponseEntity<ExamResponseDTO> publishExam(@PathVariable UUID uuid) {
        return ResponseEntity.ok(examService.publishExam(uuid));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteExam(@PathVariable UUID uuid) {
        examService.deleteExam(uuid);
        return ResponseEntity.noContent().build();
    }
}