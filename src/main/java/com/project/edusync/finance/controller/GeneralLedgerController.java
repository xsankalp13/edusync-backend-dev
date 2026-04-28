package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.gl.JournalEntryRequestDTO;
import com.project.edusync.finance.dto.gl.JournalEntryResponseDTO;
import com.project.edusync.finance.dto.gl.TrialBalanceRowDTO;
import com.project.edusync.finance.model.enums.JournalEntryStatus;
import com.project.edusync.finance.service.GeneralLedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for the General Ledger (GL).
 * Base path: /auth/finance/gl
 */
@RestController
@RequestMapping("${api.url}/auth/finance/gl")
@RequiredArgsConstructor
public class GeneralLedgerController {

    private final GeneralLedgerService glService;

    private static final Long DEFAULT_SCHOOL_ID = 1L;

    /**
     * POST /auth/finance/gl/journal-entries
     * Create and immediately post a manual journal entry.
     */
    @PostMapping("/journal-entries")
    @PreAuthorize("hasAnyAuthority('finance:gl:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<JournalEntryResponseDTO> createManualEntry(
            @Valid @RequestBody JournalEntryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(glService.createAndPostManualEntry(dto, DEFAULT_SCHOOL_ID));
    }

    /**
     * GET /auth/finance/gl/journal-entries
     * Paginated list of journal entries with optional filters.
     *
     * @param status Filter by status (DRAFT/POSTED/REVERSED)
     * @param from   Filter by entry date >= from
     * @param to     Filter by entry date <= to
     * @param page   Page number (0-indexed)
     * @param size   Page size
     */
    @GetMapping("/journal-entries")
    @PreAuthorize("hasAnyAuthority('finance:gl:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<Page<JournalEntryResponseDTO>> getJournalEntries(
            @RequestParam(required = false) JournalEntryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("entryDate").descending());
        return ResponseEntity.ok(glService.getJournalEntries(DEFAULT_SCHOOL_ID, status, from, to, pageable));
    }

    /**
     * GET /auth/finance/gl/journal-entries/{id}
     * Full detail of a single journal entry including all lines.
     */
    @GetMapping("/journal-entries/{id}")
    @PreAuthorize("hasAnyAuthority('finance:gl:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<JournalEntryResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(glService.getJournalEntryById(id, DEFAULT_SCHOOL_ID));
    }

    /**
     * POST /auth/finance/gl/journal-entries/{id}/reverse
     * Reverse a posted journal entry.
     */
    @PostMapping("/journal-entries/{id}/reverse")
    @PreAuthorize("hasAnyAuthority('finance:gl:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<JournalEntryResponseDTO> reverseEntry(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(glService.reverseEntry(id, reason, DEFAULT_SCHOOL_ID));
    }

    /**
     * GET /auth/finance/gl/trial-balance
     * Returns the Trial Balance — all account balances at current state.
     */
    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyAuthority('finance:gl:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<List<TrialBalanceRowDTO>> getTrialBalance() {
        return ResponseEntity.ok(glService.getTrialBalance(DEFAULT_SCHOOL_ID));
    }

    /**
     * GET /auth/finance/gl/ledger/{accountId}
     * Drilldown ledger for a specific account showing all postings in date range.
     */
    @GetMapping("/ledger/{accountId}")
    @PreAuthorize("hasAnyAuthority('finance:gl:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<List<JournalEntryResponseDTO>> getAccountLedger(
            @PathVariable Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(glService.getAccountLedger(accountId, from, to, DEFAULT_SCHOOL_ID));
    }
}
