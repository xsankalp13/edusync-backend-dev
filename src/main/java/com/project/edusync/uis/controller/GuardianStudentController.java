package com.project.edusync.uis.controller;

import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.StudentGuardianRelationship;
import com.project.edusync.uis.repository.GuardianRepository;
import com.project.edusync.uis.repository.StudentGuardianRelationshipRepository;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.uis.model.dto.profile.ComprehensiveUserProfileResponseDTO;
import com.project.edusync.uis.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.url}/guardian/students")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guardian Student", description = "Endpoints for guardians to view their linked students' details")
public class GuardianStudentController {
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRelationshipRepository relationshipRepository;
    private final ProfileService profileService;
    private final AuthUtil authUtil;

    @GetMapping
    @PreAuthorize("hasRole('GUARDIAN')")
    @Operation(summary = "Get all linked students", description = "Returns all students linked to the logged-in guardian.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of students fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<ComprehensiveUserProfileResponseDTO>> getLinkedStudents() {
        Long userId = authUtil.getCurrentUserId();
        Guardian guardian = guardianRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new RuntimeException("Guardian not found for userId=" + userId));
        List<StudentGuardianRelationship> relationships = relationshipRepository.findByGuardian(guardian);
        List<ComprehensiveUserProfileResponseDTO> students = relationships.stream()
                .map(rel -> profileService.getProfileByUserId(rel.getStudent().getUserProfile().getUser().getId().longValue()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{studentUuid}")
    @PreAuthorize("hasRole('GUARDIAN')")
    @Operation(summary = "Get student details", description = "Returns full details for a specific student linked to the guardian.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student details fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Student not found or not linked to guardian")
    })
    public ResponseEntity<ComprehensiveUserProfileResponseDTO> getStudentDetails(@PathVariable UUID studentUuid) {
        Long userId = authUtil.getCurrentUserId();
        Guardian guardian = guardianRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new RuntimeException("Guardian not found for userId=" + userId));
        boolean linked = relationshipRepository.findByGuardian(guardian).stream()
                .anyMatch(rel -> rel.getStudent().getUuid().equals(studentUuid));
        if (!linked) {
            return ResponseEntity.status(404).build();
        }
        // Find the student entity and get their userId
        return relationshipRepository.findByGuardian(guardian).stream()
                .filter(rel -> rel.getStudent().getUuid().equals(studentUuid))
                .findFirst()
                .map(rel -> profileService.getProfileByUserId(rel.getStudent().getUserProfile().getUser().getId().longValue()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }
}


