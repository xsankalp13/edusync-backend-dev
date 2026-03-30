package com.project.edusync.iam.controller;

import com.project.edusync.iam.model.dto.BulkTeacherSubjectAssignmentRequestDTO;
import com.project.edusync.iam.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.url}/auth/teachers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Subject Management", description = "Manage teacher teachable subjects for timetable allocation")
public class TeacherSubjectController {

    private final UserManagementService userManagementService;

    @PutMapping("/bulk-subjects")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Bulk assign subject to teachers",
            description = "Assigns one subject to multiple teacher IDs in a single operation and invalidates global timetable editor caches."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk assignment completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Subject or teacher not found")
    })
    public ResponseEntity<String> bulkAssignSubjectToTeachers(@Valid @RequestBody BulkTeacherSubjectAssignmentRequestDTO request) {
        log.info("API Request: Bulk assign subject [{}] to teacherIds={}", request.getSubjectId(), request.getTeacherIds());
        userManagementService.bulkAssignSubjectToTeachers(request);
        return ResponseEntity.ok("Bulk subject assignment completed successfully.");
    }
}

