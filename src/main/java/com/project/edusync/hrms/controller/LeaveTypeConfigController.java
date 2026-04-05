package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.leave.LeaveTypeConfigCreateDTO;
import com.project.edusync.hrms.dto.leave.LeaveTypeConfigResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveTypeConfigUpdateDTO;
import com.project.edusync.hrms.service.LeaveTypeConfigService;
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
@RequestMapping("${api.url}/auth/hrms/leaves/types")
@RequiredArgsConstructor
@Tag(name = "HRMS Leave Management", description = "Leave type master configuration APIs")
public class LeaveTypeConfigController {

    private final LeaveTypeConfigService leaveTypeConfigService;

    @GetMapping
    @Operation(summary = "Get all leave types")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<LeaveTypeConfigResponseDTO>> getAll(@RequestParam(required = false) StaffCategory category) {
        return ResponseEntity.ok(leaveTypeConfigService.getAll(category));
    }

    @GetMapping("/{identifier}")
    @Operation(summary = "Get leave type by identifier")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<LeaveTypeConfigResponseDTO> getByIdentifier(@PathVariable String identifier) {
        return ResponseEntity.ok(leaveTypeConfigService.getByIdentifier(identifier));
    }

    @PostMapping
    @Operation(summary = "Create leave type")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<LeaveTypeConfigResponseDTO> create(@Valid @RequestBody LeaveTypeConfigCreateDTO dto) {
        return new ResponseEntity<>(leaveTypeConfigService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{identifier}")
    @Operation(summary = "Update leave type")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<LeaveTypeConfigResponseDTO> update(
            @PathVariable String identifier,
            @Valid @RequestBody LeaveTypeConfigUpdateDTO dto
    ) {
        return ResponseEntity.ok(leaveTypeConfigService.updateByIdentifier(identifier, dto));
    }

    @DeleteMapping("/{identifier}")
    @Operation(summary = "Soft delete leave type")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        leaveTypeConfigService.deleteByIdentifier(identifier);
        return ResponseEntity.noContent().build();
    }
}
