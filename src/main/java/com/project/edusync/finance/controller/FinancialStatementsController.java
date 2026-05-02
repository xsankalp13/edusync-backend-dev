package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.statements.FinancialStatementDTO;
import com.project.edusync.finance.service.implementation.FinancialStatementsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("${api.url}/auth/finance/statements")
@RequiredArgsConstructor
public class FinancialStatementsController {

    private final FinancialStatementsServiceImpl statementService;
    private static final Long SID = 1L;

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyAuthority('finance:reports:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<FinancialStatementDTO> getTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(statementService.generateTrialBalance(asOfDate, SID));
    }

    @GetMapping("/profit-and-loss")
    @PreAuthorize("hasAnyAuthority('finance:reports:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<FinancialStatementDTO> getProfitAndLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statementService.generateProfitAndLoss(startDate, endDate, SID));
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAnyAuthority('finance:reports:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<FinancialStatementDTO> getBalanceSheet(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(statementService.generateBalanceSheet(asOfDate, SID));
    }
}
