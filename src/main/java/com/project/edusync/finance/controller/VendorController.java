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

/** /auth/finance/vendors */
@RestController @RequestMapping("${api.url}/auth/finance/vendors") @RequiredArgsConstructor
public class VendorController {
    private final ProcurementServiceImpl procurementService;
    private static final Long SID = 1L;

    @GetMapping @PreAuthorize("hasAnyAuthority('finance:procurement:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<VendorResponseDTO>> getAll(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? procurementService.getActiveVendors(SID) : procurementService.getAllVendors(SID));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('finance:procurement:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<VendorResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.getVendorById(id, SID));
    }

    @PostMapping @PreAuthorize("hasAnyAuthority('finance:procurement:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<VendorResponseDTO> create(@Valid @RequestBody VendorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procurementService.createVendor(dto, SID));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('finance:procurement:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<VendorResponseDTO> update(@PathVariable Long id, @Valid @RequestBody VendorRequestDTO dto) {
        return ResponseEntity.ok(procurementService.updateVendor(id, dto, SID));
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAnyAuthority('finance:procurement:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        procurementService.deactivateVendor(id, SID); return ResponseEntity.noContent().build();
    }
}
