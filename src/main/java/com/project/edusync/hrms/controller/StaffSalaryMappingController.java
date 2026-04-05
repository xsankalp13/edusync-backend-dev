package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingBulkCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingResponseDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingUpdateDTO;
import com.project.edusync.hrms.service.StaffSalaryMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/salary/mappings")
@RequiredArgsConstructor
@Tag(name = "HRMS Salary", description = "Staff salary mapping and override APIs")
public class StaffSalaryMappingController {

    private final StaffSalaryMappingService staffSalaryMappingService;

    @GetMapping
    @Operation(summary = "List staff salary mappings")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Page<StaffSalaryMappingResponseDTO>> listMappings(Pageable pageable) {
        return ResponseEntity.ok(staffSalaryMappingService.listMappings(pageable));
    }

    @GetMapping("/staff/{staffIdentifier}")
    @Operation(summary = "Get salary mappings for staff")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<StaffSalaryMappingResponseDTO>> getByStaffIdentifier(@PathVariable String staffIdentifier) {
        return ResponseEntity.ok(staffSalaryMappingService.getMappingsByStaffIdentifier(staffIdentifier));
    }

    @PostMapping
    @Operation(summary = "Create staff salary mapping")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffSalaryMappingResponseDTO> create(@Valid @RequestBody StaffSalaryMappingCreateDTO dto) {
        return new ResponseEntity<>(staffSalaryMappingService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{identifier}")
    @Operation(summary = "Update staff salary mapping")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffSalaryMappingResponseDTO> update(
            @PathVariable String identifier,
            @Valid @RequestBody StaffSalaryMappingUpdateDTO dto
    ) {
        return ResponseEntity.ok(staffSalaryMappingService.updateByIdentifier(identifier, dto));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk create staff salary mappings")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<BulkOperationResultDTO> bulkCreate(@Valid @RequestBody StaffSalaryMappingBulkCreateDTO dto) {
        return new ResponseEntity<>(staffSalaryMappingService.bulkCreate(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{identifier}/computed")
    @Operation(summary = "Get computed salary breakdown")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ComputedSalaryBreakdownDTO> computed(@PathVariable String identifier) {
        return ResponseEntity.ok(staffSalaryMappingService.computeBreakdownByIdentifier(identifier));
    }
}

