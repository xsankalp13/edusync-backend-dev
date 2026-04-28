package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.bank.BankAccountResponseDTO;
import com.project.edusync.finance.dto.bank.BankTransactionRequestDTO;
import com.project.edusync.finance.dto.bank.BankTransactionResponseDTO;
import com.project.edusync.finance.service.implementation.BankReconciliationServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** /auth/finance/bank */
@RestController @RequestMapping("${api.url}/auth/finance/bank") @RequiredArgsConstructor
public class BankReconciliationController {

    private final BankReconciliationServiceImpl bankService;
    private static final Long SID = 1L;

    // ── Bank Accounts ─────────────────────────────────────────────────────────

    @GetMapping("/accounts") @PreAuthorize("hasAnyAuthority('finance:bank:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<BankAccountResponseDTO>> getAccounts() { return ResponseEntity.ok(bankService.getAllBankAccounts(SID)); }

    @PostMapping("/accounts") @PreAuthorize("hasAnyAuthority('finance:bank:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BankAccountResponseDTO> createAccount(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankService.createBankAccount(
                (String) body.get("accountName"), (String) body.get("accountNumber"),
                (String) body.get("bankName"), (String) body.get("branchName"),
                (String) body.get("ifscCode"), (String) body.get("accountType"),
                body.get("coaAccountId") != null ? Long.parseLong(body.get("coaAccountId").toString()) : null,
                (String) body.get("notes"), SID));
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    @GetMapping("/accounts/{bankAccountId}/transactions") @PreAuthorize("hasAnyAuthority('finance:bank:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<BankTransactionResponseDTO>> getTransactions(
            @PathVariable Long bankAccountId, @RequestParam(required = false) String status) {
        return ResponseEntity.ok(bankService.getTransactions(bankAccountId, status));
    }

    @PostMapping("/transactions") @PreAuthorize("hasAnyAuthority('finance:bank:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BankTransactionResponseDTO> addTransaction(@Valid @RequestBody BankTransactionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankService.addTransaction(dto, SID));
    }

    // ── Reconciliation ────────────────────────────────────────────────────────

    @PostMapping("/accounts/{bankAccountId}/auto-match") @PreAuthorize("hasAnyAuthority('finance:bank:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<Map<String, Integer>> autoMatch(@PathVariable Long bankAccountId) {
        int count = bankService.runAutoMatch(bankAccountId, SID);
        return ResponseEntity.ok(Map.of("matched", count));
    }

    @PostMapping("/transactions/{txId}/manual-match") @PreAuthorize("hasAnyAuthority('finance:bank:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BankTransactionResponseDTO> manualMatch(
            @PathVariable Long txId, @RequestParam Long glEntryId, @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(bankService.manualMatch(txId, glEntryId, notes, SID));
    }

    @PostMapping("/transactions/{txId}/flag-exception") @PreAuthorize("hasAnyAuthority('finance:bank:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BankTransactionResponseDTO> flagException(@PathVariable Long txId, @RequestParam String reason) {
        return ResponseEntity.ok(bankService.flagException(txId, reason));
    }

    @PostMapping("/transactions/{txId}/ignore") @PreAuthorize("hasAnyAuthority('finance:bank:write','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BankTransactionResponseDTO> ignore(@PathVariable Long txId) {
        return ResponseEntity.ok(bankService.ignore(txId));
    }
}
