package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.training.TrainingDTOs.*;
import com.project.edusync.hrms.model.enums.CourseStatus;
import com.project.edusync.hrms.service.impl.TrainingServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/hrms/training")
@RequiredArgsConstructor
@Tag(name = "HRMS Training", description = "Training & Development management")
public class TrainingController {

    private final TrainingServiceImpl trainingService;

    @PostMapping("/courses")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Create training course")
    public ResponseEntity<CourseResponseDTO> create(@RequestBody CourseCreateDTO dto) {
        return ResponseEntity.ok(trainingService.createCourse(dto));
    }

    @GetMapping("/courses")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List courses")
    public ResponseEntity<List<CourseResponseDTO>> list() {
        return ResponseEntity.ok(trainingService.listCourses());
    }

    @GetMapping("/courses/{uuid}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get course")
    public ResponseEntity<CourseResponseDTO> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(trainingService.getCourse(uuid));
    }

    @PatchMapping("/courses/{uuid}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Update course status")
    public ResponseEntity<CourseResponseDTO> updateStatus(@PathVariable UUID uuid, @RequestParam CourseStatus status) {
        return ResponseEntity.ok(trainingService.updateCourseStatus(uuid, status));
    }

    @PostMapping("/courses/{uuid}/enroll")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Enroll staff in course")
    public ResponseEntity<EnrollmentResponseDTO> enroll(@PathVariable UUID uuid, @RequestBody EnrollmentCreateDTO dto) {
        return ResponseEntity.ok(trainingService.enroll(uuid, dto));
    }

    @GetMapping("/courses/{uuid}/enrollments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List enrollments for course")
    public ResponseEntity<List<EnrollmentResponseDTO>> listEnrollments(@PathVariable UUID uuid) {
        return ResponseEntity.ok(trainingService.listEnrollments(uuid));
    }

    @PostMapping("/courses/{uuid}/enrollments/{enrollmentId}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Mark enrollment complete")
    public ResponseEntity<EnrollmentResponseDTO> complete(@PathVariable UUID uuid, @PathVariable Long enrollmentId,
                                                           @RequestBody(required = false) EnrollmentCompleteDTO dto) {
        return ResponseEntity.ok(trainingService.completeEnrollment(uuid, enrollmentId, dto != null ? dto : new EnrollmentCompleteDTO(null)));
    }

    @PostMapping("/certificates")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Upload certificate")
    public ResponseEntity<CertificateResponseDTO> uploadCertificate(@RequestBody CertificateUploadDTO dto) {
        return ResponseEntity.ok(trainingService.addCertificate(dto));
    }
}

