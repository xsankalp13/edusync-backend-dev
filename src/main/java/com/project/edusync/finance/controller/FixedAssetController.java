package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.asset.*;
import com.project.edusync.finance.model.enums.AssetStatus;
import com.project.edusync.finance.service.implementation.FixedAssetServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** /auth/finance/assets */
@RestController @RequestMapping("${api.url}/auth/finance/assets") @RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetServiceImpl assetService;
    private static final Long SID = 1L;

    @GetMapping @PreAuthorize("hasAnyAuthority('finance:assets:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<AssetResponseDTO>> getAll() { return ResponseEntity.ok(assetService.getAllAssets(SID)); }

    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('finance:assets:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<AssetResponseDTO> getById(@PathVariable Long id) { return ResponseEntity.ok(assetService.getAssetById(id, SID)); }

    @PostMapping @PreAuthorize("hasAnyAuthority('finance:assets:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<AssetResponseDTO> create(@Valid @RequestBody AssetRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.createAsset(dto, SID));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAnyAuthority('finance:assets:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<AssetResponseDTO> update(@PathVariable Long id, @Valid @RequestBody AssetRequestDTO dto) {
        return ResponseEntity.ok(assetService.updateAsset(id, dto, SID));
    }

    @PostMapping("/{id}/depreciate") @PreAuthorize("hasAnyAuthority('finance:assets:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<DepreciationEntryResponseDTO> depreciate(
            @PathVariable Long id, @RequestParam String financialYear) {
        return ResponseEntity.ok(assetService.postDepreciation(id, financialYear, SID));
    }

    @PostMapping("/depreciate-batch") @PreAuthorize("hasAnyAuthority('finance:assets:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<DepreciationEntryResponseDTO>> depreciationBatch(@RequestParam String financialYear) {
        return ResponseEntity.ok(assetService.runDepreciationBatch(financialYear, SID));
    }

    @GetMapping("/{id}/depreciation") @PreAuthorize("hasAnyAuthority('finance:assets:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<DepreciationEntryResponseDTO>> getDepreciationHistory(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getDepreciationHistory(id, SID));
    }

    @PostMapping("/{id}/dispose") @PreAuthorize("hasAnyAuthority('finance:assets:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<AssetResponseDTO> dispose(
            @PathVariable Long id,
            @RequestParam(required = false) String disposalDate,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) BigDecimal proceeds) {
        LocalDate date = disposalDate != null ? LocalDate.parse(disposalDate) : LocalDate.now();
        return ResponseEntity.ok(assetService.disposeAsset(id, date, reason, proceeds, SID));
    }
}
