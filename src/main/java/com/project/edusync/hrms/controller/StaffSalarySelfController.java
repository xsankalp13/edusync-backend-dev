package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.service.StaffSalaryMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.url}/auth/hrms/salary")
@RequiredArgsConstructor
@Tag(name = "HRMS Salary", description = "Salary self-service APIs")
public class StaffSalarySelfController {

    private final StaffSalaryMappingService staffSalaryMappingService;

    @GetMapping("/self/structure")
    @Operation(summary = "Get my salary structure")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_PRINCIPAL','ROLE_LIBRARIAN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ComputedSalaryBreakdownDTO> getMyStructure() {
        return ResponseEntity.ok(staffSalaryMappingService.getMyComputedBreakdown());
    }
}

