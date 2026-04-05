package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.payroll.PayslipDetailDTO;
import com.project.edusync.hrms.dto.payroll.PayslipSummaryDTO;
import com.project.edusync.hrms.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.url}/auth/hrms/payroll")
@RequiredArgsConstructor
@Tag(name = "HRMS Payroll", description = "Payslip read APIs")
public class PayrollPayslipController {

    private final PayrollService payrollService;

    @GetMapping("/runs/{identifier}/payslips")
    @Operation(summary = "Get payslips for payroll run")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Page<PayslipSummaryDTO>> listByRun(@PathVariable String identifier, Pageable pageable) {
        return ResponseEntity.ok(payrollService.listPayslipsByRunIdentifier(identifier, pageable));
    }

    @GetMapping("/payslips/{identifier}")
    @Operation(summary = "Get payslip details")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<PayslipDetailDTO> getByIdentifier(@PathVariable String identifier) {
        return ResponseEntity.ok(payrollService.getPayslipByIdentifier(identifier));
    }

    @GetMapping("/payslips/{identifier}/pdf")
    @Operation(summary = "Download payslip PDF")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<byte[]> getPayslipPdf(@PathVariable String identifier) {
        byte[] pdf = payrollService.getPayslipPdfByIdentifier(identifier);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"payslip-" + identifier + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/staff/{staffIdentifier}/payslips")
    @Operation(summary = "Get payslip history for staff")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Page<PayslipSummaryDTO>> listByStaff(@PathVariable String staffIdentifier, Pageable pageable) {
        return ResponseEntity.ok(payrollService.listPayslipsByStaffIdentifier(staffIdentifier, pageable));
    }

    @GetMapping("/self/payslips")
    @Operation(summary = "Get my payslip history")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_PRINCIPAL','ROLE_LIBRARIAN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<PayslipSummaryDTO>> listMine(Pageable pageable) {
        return ResponseEntity.ok(payrollService.listMyPayslips(pageable));
    }

    @GetMapping("/self/payslips/{identifier}")
    @Operation(summary = "Get my payslip detail")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_PRINCIPAL','ROLE_LIBRARIAN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<PayslipDetailDTO> getMineByIdentifier(@PathVariable String identifier) {
        return ResponseEntity.ok(payrollService.getMyPayslipByIdentifier(identifier));
    }

    @GetMapping("/self/payslips/{identifier}/pdf")
    @Operation(summary = "Download my payslip PDF")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_PRINCIPAL','ROLE_LIBRARIAN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<byte[]> getMyPayslipPdf(@PathVariable String identifier) {
        byte[] pdf = payrollService.getMyPayslipPdfByIdentifier(identifier);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"my-payslip-" + identifier + ".pdf\"")
                .body(pdf);
    }
}



