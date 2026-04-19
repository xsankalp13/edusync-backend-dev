package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.exit.ExitDTOs.*;
import com.project.edusync.hrms.model.enums.ExitRequestStatus;
import com.project.edusync.hrms.service.ExitManagementService;
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
@RequestMapping("${api.url}/auth/hrms/exit")
@RequiredArgsConstructor
@Tag(name = "HRMS Exit Management", description = "Staff exit and offboarding")
public class ExitManagementController {

    private final ExitManagementService exitManagementService;

    @PostMapping("/requests")
    @Operation(summary = "Create exit request")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ExitRequestResponseDTO> create(@Valid @RequestBody ExitRequestCreateDTO dto) {
        return ResponseEntity.ok(exitManagementService.createExitRequest(dto));
    }

    @GetMapping("/requests")
    @Operation(summary = "List exit requests")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<ExitRequestResponseDTO>> list(@RequestParam(required = false) ExitRequestStatus status) {
        return ResponseEntity.ok(exitManagementService.listExitRequests(status));
    }

    @GetMapping("/requests/{uuid}")
    @Operation(summary = "Get exit request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExitRequestResponseDTO> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(exitManagementService.getExitRequest(uuid));
    }

    @PatchMapping("/requests/{uuid}/status")
    @Operation(summary = "Update exit request status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ExitRequestResponseDTO> updateStatus(@PathVariable UUID uuid, @RequestBody ExitStatusUpdateDTO dto) {
        return ResponseEntity.ok(exitManagementService.updateStatus(uuid, dto));
    }

    @PostMapping("/requests/{uuid}/clearance")
    @Operation(summary = "Add clearance item")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ClearanceItemResponseDTO> addClearance(@PathVariable UUID uuid, @RequestBody ClearanceItemCreateDTO dto) {
        return ResponseEntity.ok(exitManagementService.addClearanceItem(uuid, dto));
    }

    @GetMapping("/requests/{uuid}/clearance")
    @Operation(summary = "List clearance items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClearanceItemResponseDTO>> listClearance(@PathVariable UUID uuid) {
        return ResponseEntity.ok(exitManagementService.listClearanceItems(uuid));
    }

    @PostMapping("/requests/{uuid}/clearance/{itemId}/complete")
    @Operation(summary = "Mark clearance item done")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ClearanceItemResponseDTO> completeClearance(
            @PathVariable UUID uuid, @PathVariable Long itemId,
            @RequestParam(defaultValue = "") String completedByName,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(exitManagementService.completeClearanceItem(uuid, itemId, completedByName, remarks));
    }

    @PatchMapping("/requests/{uuid}/clearance/{itemId}/waive")
    @Operation(summary = "Waive a clearance item")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ClearanceItemResponseDTO> waivedClearance(
            @PathVariable UUID uuid, @PathVariable Long itemId,
            @RequestBody(required = false) WaiveClearanceItemDTO dto) {
        return ResponseEntity.ok(exitManagementService.waivedClearanceItem(uuid, itemId,
                dto != null ? dto : new WaiveClearanceItemDTO(null, null)));
    }

    @PostMapping("/requests/{uuid}/fnf")
    @Operation(summary = "Create full & final settlement")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<FnFResponseDTO> createFnF(@PathVariable UUID uuid, @RequestBody FnFCreateDTO dto) {
        return ResponseEntity.ok(exitManagementService.createFnF(uuid, dto));
    }

    @GetMapping("/requests/{uuid}/fnf")
    @Operation(summary = "Get full & final settlement")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FnFResponseDTO> getFnF(@PathVariable UUID uuid) {
        return ResponseEntity.ok(exitManagementService.getFnF(uuid));
    }

    @PatchMapping("/requests/{uuid}/fnf/status")
    @Operation(summary = "Update FnF status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<FnFResponseDTO> updateFnFStatus(@PathVariable UUID uuid, @RequestBody FnFStatusUpdateDTO dto) {
        return ResponseEntity.ok(exitManagementService.updateFnFStatus(uuid, dto));
    }
}

