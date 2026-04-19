package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.onboarding.OnboardingDTOs.*;
import com.project.edusync.hrms.model.enums.OnboardingStatus;
import com.project.edusync.hrms.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/hrms/onboarding")
@RequiredArgsConstructor
@Tag(name = "HRMS Onboarding", description = "Staff onboarding management")
public class OnboardingController {

    private final OnboardingService onboardingService;

    // --- Template endpoints ---
    @PostMapping("/templates")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Create onboarding template")
    public ResponseEntity<TemplateResponseDTO> createTemplate(@Valid @RequestBody TemplateCreateDTO dto) {
        return ResponseEntity.ok(onboardingService.createTemplate(dto));
    }

    @PutMapping("/templates/{uuid}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Update onboarding template")
    public ResponseEntity<TemplateResponseDTO> updateTemplate(@PathVariable UUID uuid, @Valid @RequestBody TemplateCreateDTO dto) {
        return ResponseEntity.ok(onboardingService.updateTemplate(uuid, dto));
    }

    @GetMapping("/templates")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List onboarding templates")
    public ResponseEntity<List<TemplateResponseDTO>> listTemplates() {
        return ResponseEntity.ok(onboardingService.listTemplates());
    }

    @DeleteMapping("/templates/{uuid}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Delete onboarding template")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID uuid) {
        onboardingService.deleteTemplate(uuid);
        return ResponseEntity.noContent().build();
    }

    // --- Record endpoints ---
    @PostMapping("/records")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Start onboarding for staff")
    public ResponseEntity<RecordResponseDTO> createRecord(@Valid @RequestBody RecordCreateDTO dto) {
        return ResponseEntity.ok(onboardingService.createRecord(dto));
    }

    @GetMapping("/records")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List onboarding records")
    public ResponseEntity<List<RecordResponseDTO>> listRecords(
            @RequestParam(required = false) String staffRef,
            @RequestParam(required = false) OnboardingStatus status) {
        return ResponseEntity.ok(onboardingService.listRecords(staffRef, status));
    }

    @GetMapping("/records/{uuid}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get onboarding record detail")
    public ResponseEntity<RecordResponseDTO> getRecord(@PathVariable UUID uuid) {
        return ResponseEntity.ok(onboardingService.getRecord(uuid));
    }

    @GetMapping("/staff/{staffRef}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get active onboarding record for a staff member")
    public ResponseEntity<RecordResponseDTO> getStaffOnboarding(@PathVariable String staffRef) {
        return ResponseEntity.ok(onboardingService.getStaffOnboarding(staffRef));
    }

    @PostMapping("/records/{recordUuid}/tasks/{taskId}/complete")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark an onboarding task as complete")
    public ResponseEntity<RecordResponseDTO> completeTask(
            @PathVariable UUID recordUuid,
            @PathVariable Long taskId,
            @RequestBody(required = false) CompleteTaskDTO dto) {
        return ResponseEntity.ok(onboardingService.completeTask(recordUuid, taskId, null, dto != null ? dto.remarks() : null));
    }
}

