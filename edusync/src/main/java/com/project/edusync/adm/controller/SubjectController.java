package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.request.SubjectRequestDto;
import com.project.edusync.adm.model.dto.response.SubjectResponseDto;
import com.project.edusync.adm.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Subjects.
 * All responses are wrapped in ResponseEntity for full control over the HTTP response.
 */
@RestController
@RequestMapping("${api.url}/auth") // Following your existing request mapping
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    /**
     * Creates a new subject (e.g., "Physics").
     * HTTP 201 Created on success.
     */
    @PostMapping("/subjects")
    public ResponseEntity<SubjectResponseDto> createSubject(
            @Valid @RequestBody SubjectRequestDto requestDto) {

        SubjectResponseDto createdSubject = subjectService.addSubject(requestDto);
        return new ResponseEntity<>(createdSubject, HttpStatus.CREATED);
    }

    /**
     * Retrieves a list of all active subjects.
     * HTTP 200 OK on success.
     */
    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectResponseDto>> getAllSubjects() {
        List<SubjectResponseDto> response = subjectService.getAllSubjects();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves details for a single subject by its UUID.
     * HTTP 200 OK on success.
     */
    @GetMapping("/subjects/{subjectId}")
    public ResponseEntity<SubjectResponseDto> getSubjectById(
            @PathVariable UUID subjectId) {

        SubjectResponseDto response = subjectService.getSubjectById(subjectId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Updates a subject's details by its UUID.
     * HTTP 200 OK on success.
     */
    @PutMapping("/subjects/{subjectId}")
    public ResponseEntity<SubjectResponseDto> updateSubjectById(
            @PathVariable UUID subjectId,
            @Valid @RequestBody SubjectRequestDto subjectRequestDto) {

        SubjectResponseDto response = subjectService.updateSubject(subjectId, subjectRequestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Soft deletes a subject by its UUID.
     * HTTP 204 No Content on success.
     */
    @DeleteMapping("/subjects/{subjectId}")
    public ResponseEntity<Void> deleteSubjectById(@PathVariable UUID subjectId) {
        subjectService.deleteSubject(subjectId);
        return ResponseEntity.noContent().build();
    }
}