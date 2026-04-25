package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.QuestionPaperRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.QuestionPaperResponseDTO;
import com.project.edusync.em.model.service.QuestionPaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/examination/papers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
@Tag(name = "Question Papers", description = "APIs for generating and managing exam question papers")
public class QuestionPaperController {

    private final QuestionPaperService questionPaperService;

    @PostMapping("/generate")
    @Operation(summary = "Generate a new question paper for a schedule")
    public ResponseEntity<QuestionPaperResponseDTO> generateQuestionPaper(
            @Valid @RequestBody QuestionPaperRequestDTO requestDTO) {
        return new ResponseEntity<>(questionPaperService.generateQuestionPaper(requestDTO), HttpStatus.CREATED);
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get a question paper by its UUID")
    public ResponseEntity<QuestionPaperResponseDTO> getQuestionPaperByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(questionPaperService.getQuestionPaperByUuid(uuid));
    }

    @GetMapping("/by-schedule/{scheduleId}")
    @Operation(summary = "Get the question paper for a specific exam schedule")
    public ResponseEntity<QuestionPaperResponseDTO> getQuestionPaperBySchedule(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(questionPaperService.getQuestionPaperByScheduleId(scheduleId));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete a question paper")
    public ResponseEntity<Void> deleteQuestionPaper(@PathVariable UUID uuid) {
        questionPaperService.deleteQuestionPaper(uuid);
        return ResponseEntity.noContent().build();
    }
}