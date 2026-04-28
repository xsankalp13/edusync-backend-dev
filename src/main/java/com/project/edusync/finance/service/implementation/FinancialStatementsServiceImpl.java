package com.project.edusync.finance.service.implementation;

import com.project.edusync.finance.dto.statements.FinancialStatementDTO;
import com.project.edusync.finance.dto.statements.FinancialStatementDTO.AccountBalanceDTO;
import com.project.edusync.finance.model.entity.Account;
import com.project.edusync.finance.repository.AccountRepository;
import com.project.edusync.finance.service.GeneralLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialStatementsServiceImpl {

    private final AccountRepository accountRepository;
    private final GeneralLedgerService glService;

    /**
     * Generates a Trial Balance as of a specific date.
     * Includes all accounts and balances up to that date.
     */
    public FinancialStatementDTO generateTrialBalance(LocalDate asOfDate, Long schoolId) {
        List<Account> accounts = accountRepository.findBySchoolId(schoolId);
        List<AccountBalanceDTO> items = new ArrayList<>();
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal balance = glService.getAccountBalanceAsOfDate(acc.getId(), asOfDate, schoolId);
            if (balance.compareTo(BigDecimal.ZERO) == 0) continue; // Skip zero balance accounts

            // Based on normal balance rule:
            // Asset/Expense: Positive means Dr, Negative means Cr
            // Liability/Equity/Revenue: Positive means Cr, Negative means Dr
            BigDecimal dr = BigDecimal.ZERO;
            BigDecimal cr = BigDecimal.ZERO;
            
            boolean isNormalDebit = "ASSET".equals(acc.getAccountType().name()) || "EXPENSE".equals(acc.getAccountType().name());
            
            if (isNormalDebit) {
                if (balance.compareTo(BigDecimal.ZERO) > 0) dr = balance;
                else cr = balance.abs();
            } else {
                if (balance.compareTo(BigDecimal.ZERO) > 0) cr = balance;
                else dr = balance.abs();
            }
            
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
            
            items.add(new AccountBalanceDTO(acc.getId(), acc.getCode(), acc.getName(), acc.getAccountType().name(), dr, cr, balance));
        }

        return new FinancialStatementDTO("Trial Balance as of " + asOfDate, items, totalDr, totalCr, null);
    }

    /**
     * Generates a Profit & Loss (Income Statement) for a period.
     * Includes only REVENUE and EXPENSE accounts.
     */
    public FinancialStatementDTO generateProfitAndLoss(LocalDate startDate, LocalDate endDate, Long schoolId) {
        List<Account> accounts = accountRepository.findBySchoolId(schoolId).stream()
            .filter(a -> "REVENUE".equals(a.getAccountType().name()) || "EXPENSE".equals(a.getAccountType().name()) || "INCOME".equals(a.getAccountType().name()))
            .collect(Collectors.toList());
            
        List<AccountBalanceDTO> items = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Account acc : accounts) {
            // Net balance for period = Balance(end) - Balance(start - 1 day)
            BigDecimal endBal = glService.getAccountBalanceAsOfDate(acc.getId(), endDate, schoolId);
            BigDecimal startBal = glService.getAccountBalanceAsOfDate(acc.getId(), startDate.minusDays(1), schoolId);
            BigDecimal periodActivity = endBal.subtract(startBal); // Simplistic way. Assumes balances are cumulative.
            
            if (periodActivity.compareTo(BigDecimal.ZERO) == 0) continue;
            
            if ("REVENUE".equals(acc.getAccountType().name()) || "INCOME".equals(acc.getAccountType().name())) {
                totalRevenue = totalRevenue.add(periodActivity); // Positive implies Revenue increase
            } else {
                totalExpense = totalExpense.add(periodActivity); // Positive implies Expense increase
            }
            
            items.add(new AccountBalanceDTO(acc.getId(), acc.getCode(), acc.getName(), acc.getAccountType().name(), null, null, periodActivity));
        }

        BigDecimal netProfit = totalRevenue.subtract(totalExpense);
        return new FinancialStatementDTO("Profit & Loss from " + startDate + " to " + endDate, items, totalExpense, totalRevenue, netProfit);
    }

    /**
     * Generates a Balance Sheet as of a specific date.
     * Includes ASSET, LIABILITY, and EQUITY accounts.
     */
    public FinancialStatementDTO generateBalanceSheet(LocalDate asOfDate, Long schoolId) {
        List<Account> accounts = accountRepository.findBySchoolId(schoolId).stream()
            .filter(a -> "ASSET".equals(a.getAccountType().name()) || "LIABILITY".equals(a.getAccountType().name()) || "EQUITY".equals(a.getAccountType().name()))
            .collect(Collectors.toList());
            
        List<AccountBalanceDTO> items = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabEq = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal balance = glService.getAccountBalanceAsOfDate(acc.getId(), asOfDate, schoolId);
            if (balance.compareTo(BigDecimal.ZERO) == 0) continue;
            
            if ("ASSET".equals(acc.getAccountType().name())) {
                totalAssets = totalAssets.add(balance);
            } else {
                totalLiabEq = totalLiabEq.add(balance);
            }
            
            items.add(new AccountBalanceDTO(acc.getId(), acc.getCode(), acc.getName(), acc.getAccountType().name(), null, null, balance));
        }
        
        // Ensure Retained Earnings (Profit & Loss up to asOfDate) is added to Equity to balance it
        FinancialStatementDTO pnl = generateProfitAndLoss(LocalDate.of(2000, 1, 1), asOfDate, schoolId); // PnL since beginning
        if (pnl.netProfitOrLoss() != null && pnl.netProfitOrLoss().compareTo(BigDecimal.ZERO) != 0) {
            items.add(new AccountBalanceDTO(null, "RETAINED", "Retained Earnings (Current Year)", "EQUITY", null, null, pnl.netProfitOrLoss()));
            totalLiabEq = totalLiabEq.add(pnl.netProfitOrLoss());
        }

        return new FinancialStatementDTO("Balance Sheet as of " + asOfDate, items, totalAssets, totalLiabEq, null); // Dr=Assets, Cr=Liab+Eq
    }
}
