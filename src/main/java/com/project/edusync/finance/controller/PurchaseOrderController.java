package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.procurement.*;
import com.project.edusync.finance.service.implementation.ProcurementServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** /auth/finance/purchase-orders */
@RestController @RequestMapping("${api.url}/auth/finance/purchase-orders") @RequiredArgsConstructor
public class PurchaseOrderController {
    private final ProcurementServiceImpl procurementService;
    private static final Long SID = 1L;

    @GetMapping @PreAuthorize("hasAnyAuthority('finance:procurement:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getAll() {
        return ResponseEntity.ok(procurementService.getAllPOs(SID));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('finance:procurement:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.getPOById(id, SID));
    }

    @PostMapping @PreAuthorize("hasAnyAuthority('finance:procurement:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> create(@Valid @RequestBody PurchaseOrderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procurementService.createPO(dto, SID));
    }

    @PostMapping("/{id}/submit") @PreAuthorize("hasAnyAuthority('finance:procurement:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> submit(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.submitPO(id, SID));
    }

    @PostMapping("/{id}/approve") @PreAuthorize("hasAnyAuthority('finance:procurement:approve','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.approvePO(id, SID));
    }

    @PostMapping("/{id}/reject") @PreAuthorize("hasAnyAuthority('finance:procurement:approve','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(procurementService.rejectPO(id, reason, SID));
    }

    @PostMapping("/{id}/cancel") @PreAuthorize("hasAnyAuthority('finance:procurement:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.cancelPO(id, SID));
    }

    @GetMapping("/{id}/grns") @PreAuthorize("hasAnyAuthority('finance:procurement:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<GRNResponseDTO>> getGRNs(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.getGRNsForPO(id, SID));
    }

    @PostMapping("/grns") @PreAuthorize("hasAnyAuthority('finance:procurement:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GRNResponseDTO> createGRN(@Valid @RequestBody GRNRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procurementService.createGRN(dto, SID));
    }

    @GetMapping("/grns") @PreAuthorize("hasAnyAuthority('finance:procurement:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<GRNResponseDTO>> getAllGRNs() {
        return ResponseEntity.ok(procurementService.getAllGRNs(SID));
    }

    @GetMapping("/grns/{id}") @PreAuthorize("hasAnyAuthority('finance:procurement:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GRNResponseDTO> getGRNById(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.getGRNById(id, SID));
    }
}
