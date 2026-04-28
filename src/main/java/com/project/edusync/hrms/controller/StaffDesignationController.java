package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.designation.BulkDesignationAssignRequestDTO;
import com.project.edusync.hrms.dto.designation.BulkDesignationAssignResultDTO;
import com.project.edusync.hrms.dto.designation.StaffDesignationCreateUpdateDTO;
import com.project.edusync.hrms.dto.designation.StaffDesignationResponseDTO;
import com.project.edusync.hrms.service.StaffDesignationService;
import com.project.edusync.uis.model.enums.StaffCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/designations")
@RequiredArgsConstructor
@Tag(name = "HRMS Staff Designation", description = "Staff designation master APIs")
public class StaffDesignationController {

    private final StaffDesignationService staffDesignationService;

    @GetMapping
    @Operation(summary = "List staff designations")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<StaffDesignationResponseDTO>> list(
            @RequestParam(required = false) StaffCategory category,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(staffDesignationService.list(category, active));
    }

    @GetMapping("/{identifier}")
    @Operation(summary = "Get staff designation by identifier")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffDesignationResponseDTO> getByIdentifier(@PathVariable String identifier) {
        return ResponseEntity.ok(staffDesignationService.getByIdentifier(identifier));
    }

    @PostMapping
    @Operation(summary = "Create staff designation")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffDesignationResponseDTO> create(@Valid @RequestBody StaffDesignationCreateUpdateDTO dto) {
        return new ResponseEntity<>(staffDesignationService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{identifier}")
    @Operation(summary = "Update staff designation")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffDesignationResponseDTO> update(
            @PathVariable String identifier,
            @Valid @RequestBody StaffDesignationCreateUpdateDTO dto
    ) {
        return ResponseEntity.ok(staffDesignationService.updateByIdentifier(identifier, dto));
    }

    @DeleteMapping("/{identifier}")
    @Operation(summary = "Soft delete staff designation")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        staffDesignationService.deleteByIdentifier(identifier);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /{identifier}/assign-staff
     *
     * Bulk-assigns a list of staff members to this designation.
     * Each entry in {@code staffRefs} may be a staff UUID or employeeId.
     * Processing is resilient — one bad ref will not abort the rest.
     *
     * <pre>
     * {
     *   "staffRefs": ["EMP001", "EMP002", "550e8400-e29b-41d4-a716-446655440000"]
     * }
     * </pre>
     */
    @PostMapping("/{identifier}/assign-staff")
    @Operation(
            summary = "Bulk assign staff to a designation",
            description = "Pass a list of staff UUIDs or employeeIds. "
                    + "Each entry is processed independently; failures are listed in the response."
    )
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<BulkDesignationAssignResultDTO> bulkAssignStaff(
            @PathVariable String identifier,
            @Valid @RequestBody BulkDesignationAssignRequestDTO dto) {
        return ResponseEntity.ok(staffDesignationService.bulkAssignToDesignation(identifier, dto));
    }

    @PostMapping("/unassign-staff")
    @Operation(
            summary = "Bulk unassign staff from their designations",
            description = "Clears designation for each staff ref. "
                    + "JobTitle reverts to StaffType name. Salary mappings are preserved."
    )
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<BulkDesignationAssignResultDTO> bulkUnassignStaff(
            @Valid @RequestBody BulkDesignationAssignRequestDTO dto) {
        return ResponseEntity.ok(staffDesignationService.bulkUnassignFromDesignation(dto));
    }
}


