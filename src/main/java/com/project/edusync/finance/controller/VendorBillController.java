package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.procurement.VendorBillRequestDTO;
import com.project.edusync.finance.dto.procurement.VendorBillResponseDTO;
import com.project.edusync.finance.model.enums.VendorBillStatus;
import com.project.edusync.finance.service.implementation.VendorBillServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

/** /auth/finance/vendor-bills */
@RestController @RequestMapping("${api.url}/auth/finance/vendor-bills") @RequiredArgsConstructor
public class VendorBillController {
    private final VendorBillServiceImpl billService;
    private static final Long SID = 1L;

    @GetMapping @PreAuthorize("hasAnyAuthority('finance:ap:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<VendorBillResponseDTO>> getAll(
            @RequestParam(required = false) VendorBillStatus status) {
        return ResponseEntity.ok(status != null ? billService.getBillsByStatus(status, SID) : billService.getAllBills(SID));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('finance:ap:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<VendorBillResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getBillById(id, SID));
    }

    @GetMapping("/overdue") @PreAuthorize("hasAnyAuthority('finance:ap:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<VendorBillResponseDTO>> getOverdue() {
        return ResponseEntity.ok(billService.getOverdueBills(SID));
    }

    @GetMapping("/outstanding-payables") @PreAuthorize("hasAnyAuthority('finance:ap:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BigDecimal> getOutstandingPayables() {
        return ResponseEntity.ok(billService.getOutstandingPayables(SID));
    }

    @PostMapping @PreAuthorize("hasAnyAuthority('finance:ap:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<VendorBillResponseDTO> create(@Valid @RequestBody VendorBillRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.createBill(dto, SID));
    }

    @PostMapping("/{id}/approve-payment") @PreAuthorize("hasAnyAuthority('finance:ap:approve','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<VendorBillResponseDTO> approveForPayment(@PathVariable Long id) {
        return ResponseEntity.ok(billService.approveBillForPayment(id, SID));
    }

    @PostMapping("/{id}/record-payment") @PreAuthorize("hasAnyAuthority('finance:ap:approve','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<VendorBillResponseDTO> recordPayment(
            @PathVariable Long id, @RequestParam(required = false) String paymentReference) {
        return ResponseEntity.ok(billService.recordPayment(id, paymentReference, SID));
    }

    @PostMapping("/{id}/override-mismatch") @PreAuthorize("hasAnyAuthority('finance:ap:approve','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<VendorBillResponseDTO> overrideMismatch(
            @PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(billService.overrideMismatch(id, reason, SID));
    }

    @PostMapping("/{id}/cancel") @PreAuthorize("hasAnyAuthority('finance:ap:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<VendorBillResponseDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(billService.cancelBill(id, SID));
    }
}
