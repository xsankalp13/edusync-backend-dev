package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.grant.*;
import com.project.edusync.finance.model.enums.GrantStatus;
import com.project.edusync.finance.service.implementation.GrantServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** /auth/finance/grants */
@RestController @RequestMapping("${api.url}/auth/finance/grants") @RequiredArgsConstructor
public class GrantController {

    private final GrantServiceImpl grantService;
    private static final Long SID = 1L;

    @GetMapping @PreAuthorize("hasAnyAuthority('finance:grants:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<GrantResponseDTO>> getAll() { return ResponseEntity.ok(grantService.getAllGrants(SID)); }

    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('finance:grants:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GrantResponseDTO> getById(@PathVariable Long id) { return ResponseEntity.ok(grantService.getGrantById(id, SID)); }

    @GetMapping("/nearing-expiry") @PreAuthorize("hasAnyAuthority('finance:grants:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<GrantResponseDTO>> nearingExpiry() { return ResponseEntity.ok(grantService.getGrantsNearingExpiry(SID)); }

    @PostMapping @PreAuthorize("hasAnyAuthority('finance:grants:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GrantResponseDTO> create(@Valid @RequestBody GrantRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grantService.createGrant(dto, SID));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('finance:grants:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GrantResponseDTO> update(@PathVariable Long id, @Valid @RequestBody GrantRequestDTO dto) {
        return ResponseEntity.ok(grantService.updateGrant(id, dto, SID));
    }

    @PostMapping("/{id}/activate") @PreAuthorize("hasAnyAuthority('finance:grants:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GrantResponseDTO> activate(@PathVariable Long id, @RequestParam(required = false) BigDecimal receivedAmount) {
        return ResponseEntity.ok(grantService.activateGrant(id, receivedAmount, SID));
    }

    @PostMapping("/{id}/status") @PreAuthorize("hasAnyAuthority('finance:grants:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GrantResponseDTO> updateStatus(@PathVariable Long id, @RequestParam GrantStatus status) {
        return ResponseEntity.ok(grantService.updateStatus(id, status, SID));
    }

    @PostMapping("/{id}/close") @PreAuthorize("hasAnyAuthority('finance:grants:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GrantResponseDTO> close(@PathVariable Long id) { return ResponseEntity.ok(grantService.closeGrant(id, SID)); }

    @PostMapping("/{id}/lapse") @PreAuthorize("hasAnyAuthority('finance:grants:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GrantResponseDTO> lapse(@PathVariable Long id) { return ResponseEntity.ok(grantService.lapsGrant(id, SID)); }

    // Utilisation
    @PostMapping("/utilisations") @PreAuthorize("hasAnyAuthority('finance:grants:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<GrantUtilizationResponseDTO> recordUtilisation(@Valid @RequestBody GrantUtilizationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grantService.recordUtilisation(dto, SID));
    }

    @GetMapping("/{id}/utilisations") @PreAuthorize("hasAnyAuthority('finance:grants:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<GrantUtilizationResponseDTO>> getUtilisations(@PathVariable Long id) {
        return ResponseEntity.ok(grantService.getUtilisations(id, SID));
    }
}
