package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.statutory.*;
import com.project.edusync.hrms.service.impl.StatutoryServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("${api.url}/auth/hrms/statutory")
@RequiredArgsConstructor
@Tag(name = "HRMS Statutory Compliance", description = "Statutory config and compliance reports")
public class StatutoryController {

    private final StatutoryServiceImpl statutoryService;

    @PostMapping("/config")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Create or update statutory config for a financial year")
    public ResponseEntity<StatutoryConfigDTO> upsertConfig(@RequestBody StatutoryConfigDTO dto) {
        return ResponseEntity.ok(statutoryService.createOrUpdateConfig(dto));
    }

    @GetMapping("/config/{financialYear}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Get statutory config for a financial year")
    public ResponseEntity<StatutoryConfigDTO> getConfig(@PathVariable String financialYear) {
        return ResponseEntity.ok(statutoryService.getConfig(financialYear));
    }

    @GetMapping("/reports/pf")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "PF contribution report")
    public ResponseEntity<PfReportDTO> pfReport(
            @RequestParam int month, @RequestParam int year,
            @RequestParam String financialYear) {
        return ResponseEntity.ok(statutoryService.getPfReport(month, year, financialYear));
    }

    @GetMapping("/reports/esi")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "ESI contribution report")
    public ResponseEntity<EsiReportDTO> esiReport(
            @RequestParam int month, @RequestParam int year,
            @RequestParam String financialYear) {
        return ResponseEntity.ok(statutoryService.getEsiReport(month, year, financialYear));
    }

    @GetMapping("/reports/pt")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Professional Tax report")
    public ResponseEntity<PtReportDTO> ptReport(
            @RequestParam int month, @RequestParam int year,
            @RequestParam String financialYear) {
        return ResponseEntity.ok(statutoryService.getPtReport(month, year, financialYear));
    }

    @GetMapping("/reports/tds")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "TDS deduction report")
    public ResponseEntity<TdsReportDTO> tdsReport(
            @RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(statutoryService.getTdsReport(month, year));
    }

    @GetMapping("/reports/salary-register")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Monthly salary register")
    public ResponseEntity<SalaryRegisterDTO> salaryRegister(
            @RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(statutoryService.getSalaryRegister(month, year));
    }

    @GetMapping("/reports/headcount")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Staff headcount report")
    public ResponseEntity<HeadcountReportDTO> headcount() {
        return ResponseEntity.ok(statutoryService.getHeadcountReport());
    }

    @GetMapping("/reports/attrition")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Attrition report")
    public ResponseEntity<AttritionReportDTO> attrition(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(statutoryService.getAttritionReport(from, to));
    }

    @GetMapping("/reports/form16")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Form 16 data")
    public ResponseEntity<Form16DataDTO> form16(@RequestParam String financialYear) {
        return ResponseEntity.ok(statutoryService.getForm16Data(financialYear));
    }

    @GetMapping("/reports/compliance-summary")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Annual compliance summary")
    public ResponseEntity<ComplianceSummaryDTO> complianceSummary(@RequestParam String financialYear) {
        return ResponseEntity.ok(statutoryService.getComplianceSummary(financialYear));
    }
}

