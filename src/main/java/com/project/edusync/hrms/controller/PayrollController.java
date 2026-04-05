package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.payroll.PayrollRunCreateDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunResponseDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunSummaryDTO;
import com.project.edusync.hrms.service.PayrollService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.url}/auth/hrms/payroll/runs")
@RequiredArgsConstructor
@Tag(name = "HRMS Payroll", description = "Monthly payroll run APIs")
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping
    @Operation(summary = "Generate payroll run for month")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<PayrollRunResponseDTO> createRun(@Valid @RequestBody PayrollRunCreateDTO dto) {
        return new ResponseEntity<>(payrollService.createRun(dto), HttpStatus.CREATED);
    }

    @PostMapping("/{identifier}/approve")
    @Operation(summary = "Approve processed payroll run")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<PayrollRunResponseDTO> approve(@PathVariable String identifier) {
        return ResponseEntity.ok(payrollService.approveRunByIdentifier(identifier));
    }

    @PostMapping("/{identifier}/disburse")
    @Operation(summary = "Mark approved payroll run as disbursed")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<PayrollRunResponseDTO> disburse(@PathVariable String identifier) {
        return ResponseEntity.ok(payrollService.disburseRunByIdentifier(identifier));
    }

    @GetMapping
    @Operation(summary = "List payroll runs")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Page<PayrollRunSummaryDTO>> listRuns(Pageable pageable) {
        return ResponseEntity.ok(payrollService.listRuns(pageable));
    }

    @GetMapping("/{identifier}")
    @Operation(summary = "Get payroll run details")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<PayrollRunResponseDTO> getByIdentifier(@PathVariable String identifier) {
        return ResponseEntity.ok(payrollService.getRunByIdentifier(identifier));
    }
}


