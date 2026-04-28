package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.misc.MiscellaneousReceiptRequestDTO;
import com.project.edusync.finance.dto.misc.MiscellaneousReceiptResponseDTO;
import com.project.edusync.finance.service.implementation.MiscellaneousReceiptServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/finance/misc-receipts")
@RequiredArgsConstructor
public class MiscellaneousReceiptController {

    private final MiscellaneousReceiptServiceImpl receiptService;
    private static final Long SID = 1L;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance:misc_receipts:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<MiscellaneousReceiptResponseDTO>> getAll() {
        return ResponseEntity.ok(receiptService.getAllReceipts(SID));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('finance:misc_receipts:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<MiscellaneousReceiptResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getReceiptById(id, SID));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('finance:misc_receipts:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<MiscellaneousReceiptResponseDTO> create(@Valid @RequestBody MiscellaneousReceiptRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.recordReceipt(dto, SID));
    }
}
