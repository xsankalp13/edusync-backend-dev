package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.request.AcademicClassRequestDto;
import com.project.edusync.adm.model.dto.response.AcademicClassResponseDto;
import com.project.edusync.adm.model.dto.request.SectionRequestDto;
import com.project.edusync.adm.model.dto.response.SectionResponseDto;

import com.project.edusync.adm.service.AcademicClassService;
import com.project.edusync.adm.service.impl.AcademicClassServiceImpl;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for managing Academic Classes and their Sections.
 * All responses are wrapped in ResponseEntity for full control over the HTTP response.
 */
@RestController
@RequestMapping("${api.url}/auth")
@RequiredArgsConstructor // Automatically injects the service via constructor
public class AcademicClassController {

    // @RequiredArgsConstructor creates a constructor for this final field
    // private final AcademicClassService academicClassService;

    // --- AcademicClass Endpoints ---

    /**
     * Creates a new academic class (e.g., "Grade 9").
     * HTTP 201 Created on success.
     */

    private final AcademicClassService academicClassService;

    @PostMapping("/classes")
    public ResponseEntity<AcademicClassResponseDto> createAcademicClass(
            @Valid @RequestBody AcademicClassRequestDto requestDto) {

        // Stubbed response - replace with your service call
        // AcademicClassResponseDto createdClass = academicClassService.createClass(requestDto);
        AcademicClassResponseDto createdClass = academicClassService.addClass(requestDto);

        return new ResponseEntity<>(createdClass, HttpStatus.CREATED);
    }

    @GetMapping("/classes")
    public ResponseEntity<List<AcademicClassResponseDto>> getAllClasses(){
        List<AcademicClassResponseDto> response = academicClassService.getAllClasses();
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @GetMapping("/classes/{classId}")
    public ResponseEntity<AcademicClassResponseDto> getClassById(@PathVariable UUID classId){
        AcademicClassResponseDto response = academicClassService.getClassById(classId);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @PutMapping("/classes/{classId}")
    public ResponseEntity<AcademicClassResponseDto> updateClassById(@PathVariable UUID classId,@Valid @RequestBody AcademicClassRequestDto academicClassRequestDto){
        AcademicClassResponseDto response = academicClassService.updateClass(classId,academicClassRequestDto);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @DeleteMapping("/classes/{classId}")
    public ResponseEntity<Void> deleteClassById(@PathVariable UUID classId){
        academicClassService.deleteClass(classId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/classes/{classId}/sections")
    public ResponseEntity<SectionResponseDto> createSectionForClass(
            @PathVariable UUID classId,
            @Valid @RequestBody SectionRequestDto requestDto) {

        SectionResponseDto createdSection = academicClassService.addSectionToClass(classId, requestDto);
        return new ResponseEntity<>(createdSection, HttpStatus.CREATED);
    }

    @GetMapping("/classes/{classId}/sections")
    public ResponseEntity<Set<SectionResponseDto>> getAllSectionsForClass(
            @PathVariable UUID classId) {

        Set<SectionResponseDto> sections = academicClassService.getAllSectionsForClass(classId);
        return ResponseEntity.ok(sections);
    }
}