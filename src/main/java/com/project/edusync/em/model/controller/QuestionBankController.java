package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.RequestDTO.QuestionBankRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.QuestionBankResponseDTO;
import com.project.edusync.em.model.enums.DifficultyLevel;
import com.project.edusync.em.model.enums.QuestionType;
import com.project.edusync.em.model.service.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("${api.url}/auth/examination/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
@Tag(name = "Question Bank", description = "APIs for managing the central repository of questions")
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @PostMapping
    @Operation(summary = "Add a new question to the bank")
    public ResponseEntity<QuestionBankResponseDTO> createQuestion(
            @Valid @RequestBody QuestionBankRequestDTO requestDTO) {
        return new ResponseEntity<>(questionBankService.createQuestion(requestDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all questions with optional filters")
    public ResponseEntity<List<QuestionBankResponseDTO>> getAllQuestions(
            @Parameter(description = "Filter by Subject UUID") @RequestParam(required = false) UUID subjectId,
            @Parameter(description = "Filter by Class UUID") @RequestParam(required = false) UUID classId,
            @Parameter(description = "Filter by Topic (partial match)") @RequestParam(required = false) String topic,
            @Parameter(description = "Filter by Question Type") @RequestParam(required = false) QuestionType type,
            @Parameter(description = "Filter by Difficulty") @RequestParam(required = false) DifficultyLevel difficulty) {

        return ResponseEntity.ok(questionBankService.getAllQuestions(subjectId, classId, topic, type, difficulty));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get a specific question by UUID")
    public ResponseEntity<QuestionBankResponseDTO> getQuestionByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(questionBankService.getQuestionByUuid(uuid));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update an existing question")
    public ResponseEntity<QuestionBankResponseDTO> updateQuestion(
            @PathVariable UUID uuid,
            @Valid @RequestBody QuestionBankRequestDTO requestDTO) {
        return ResponseEntity.ok(questionBankService.updateQuestion(uuid, requestDTO));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete a question")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID uuid) {
        questionBankService.deleteQuestion(uuid);
        return ResponseEntity.noContent().build();
    }
}