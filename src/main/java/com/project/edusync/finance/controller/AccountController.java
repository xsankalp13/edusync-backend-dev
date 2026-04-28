package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.account.AccountRequestDTO;
import com.project.edusync.finance.dto.account.AccountResponseDTO;
import com.project.edusync.finance.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Chart of Accounts (COA) management.
 * Base path: /auth/finance/accounts
 */
@RestController
@RequestMapping("${api.url}/auth/finance/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // TODO: Replace hardcoded schoolId (1L) with extraction from JWT claims
    // once multi-school isolation is finalized. For now, all ops default to school 1.
    private static final Long DEFAULT_SCHOOL_ID = 1L;

    /**
     * GET /auth/finance/accounts/tree
     * Returns the full COA as a nested tree — used by the ChartOfAccounts.tsx tree view.
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('finance:coa:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<AccountResponseDTO>> getTree() {
        return ResponseEntity.ok(accountService.getCOATree(DEFAULT_SCHOOL_ID));
    }

    /**
     * GET /auth/finance/accounts
     * Flat list of all accounts — used by admin data tables.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance:coa:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<AccountResponseDTO>> getAll() {
        return ResponseEntity.ok(accountService.getAllAccounts(DEFAULT_SCHOOL_ID));
    }

    /**
     * GET /auth/finance/accounts/posting
     * Flat list of active posting accounts — used by journal entry dropdowns.
     */
    @GetMapping("/posting")
    @PreAuthorize("hasAnyAuthority('finance:coa:read', 'finance:gl:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<AccountResponseDTO>> getPostingAccounts() {
        return ResponseEntity.ok(accountService.getPostingAccounts(DEFAULT_SCHOOL_ID));
    }

    /**
     * GET /auth/finance/accounts/{id}
     * Single account detail.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('finance:coa:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<AccountResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id, DEFAULT_SCHOOL_ID));
    }

    /**
     * POST /auth/finance/accounts
     * Create a new account in the COA.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('finance:coa:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<AccountResponseDTO> create(@Valid @RequestBody AccountRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(dto, DEFAULT_SCHOOL_ID));
    }

    /**
     * PUT /auth/finance/accounts/{id}
     * Update an existing account.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('finance:coa:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<AccountResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequestDTO dto) {
        return ResponseEntity.ok(accountService.updateAccount(id, dto, DEFAULT_SCHOOL_ID));
    }

    /**
     * DELETE /auth/finance/accounts/{id}
     * Soft-deletes (deactivates) an account.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('finance:coa:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        accountService.deactivateAccount(id, DEFAULT_SCHOOL_ID);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /auth/finance/accounts/seed
     * Seeds the default Indian educational institution COA for this school.
     * Should be called once during school setup.
     */
    @PostMapping("/seed")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<String> seedCOA() {
        accountService.seedDefaultCOA(DEFAULT_SCHOOL_ID);
        return ResponseEntity.ok("Default Chart of Accounts seeded successfully.");
    }
}
