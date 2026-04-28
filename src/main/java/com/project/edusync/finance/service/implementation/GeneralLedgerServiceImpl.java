package com.project.edusync.finance.service.implementation;

import com.project.edusync.finance.dto.gl.*;
import com.project.edusync.finance.model.entity.Account;
import com.project.edusync.finance.model.entity.JournalEntry;
import com.project.edusync.finance.model.entity.JournalLine;
import com.project.edusync.finance.model.enums.AccountType;
import com.project.edusync.finance.model.enums.JournalEntryStatus;
import com.project.edusync.finance.model.enums.JournalReferenceType;
import com.project.edusync.finance.repository.AccountRepository;
import com.project.edusync.finance.repository.JournalEntryRepository;
import com.project.edusync.finance.repository.JournalLineRepository;
import com.project.edusync.finance.service.GeneralLedgerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GeneralLedgerServiceImpl implements GeneralLedgerService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;

    @Override
    public JournalEntryResponseDTO createAndPostManualEntry(JournalEntryRequestDTO dto, Long schoolId) {
        validateLines(dto.lines());

        JournalEntry entry = buildEntry(
                dto.entryDate(),
                dto.description(),
                dto.referenceType() != null ? dto.referenceType() : JournalReferenceType.MANUAL,
                dto.referenceId(),
                schoolId
        );

        int lineNum = 1;
        for (JournalLineRequestDTO lineDTO : dto.lines()) {
            Account account = findActivePostingAccount(lineDTO.accountId(), schoolId);
            JournalLine line = new JournalLine();
            line.setAccount(account);
            line.setDebitAmount(nvl(lineDTO.debitAmount()));
            line.setCreditAmount(nvl(lineDTO.creditAmount()));
            line.setNarration(lineDTO.narration());
            line.setLineNumber(lineNum++);
            entry.addLine(line);
        }

        postEntry(entry);
        return toResponseDTO(journalEntryRepository.save(entry));
    }

    @Override
    public JournalEntry createJournalEntry(
            LocalDate entryDate,
            String description,
            JournalReferenceType referenceType,
            Long referenceId,
            List<JournalLine> lines,
            Long schoolId
    ) {
        JournalEntry entry = buildEntry(entryDate, description, referenceType, referenceId, schoolId);
        
        int lineNum = 1;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        
        for (JournalLine line : lines) {
            line.setLineNumber(lineNum++);
            if (line.getDebitAmount() == null) line.setDebitAmount(BigDecimal.ZERO);
            if (line.getCreditAmount() == null) line.setCreditAmount(BigDecimal.ZERO);
            
            // if both are zero but "amount" was set logic fallback 
            // since we are manually constructing it in Misc/Payroll.
            // Wait, we used `line.setIsDebit(true)` and `line.setAmount()` in other services! 
            // Oh, JournalLine doesn't have isDebit or setAmount!
            
            totalDebits = totalDebits.add(line.getDebitAmount());
            totalCredits = totalCredits.add(line.getCreditAmount());
            entry.addLine(line);
        }
        
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException("Journal Entry must balance. Dr: " + totalDebits + ", Cr: " + totalCredits);
        }

        postEntry(entry);
        JournalEntry saved = journalEntryRepository.save(entry);
        updateAccountBalances(saved);
        return saved;
    }

    @Override
    public JournalEntry autoPostEntry(
            LocalDate entryDate,
            String description,
            JournalReferenceType referenceType,
            Long referenceId,
            Long debitAccountId,
            Long creditAccountId,
            BigDecimal amount,
            Long schoolId
    ) {
        // Idempotency guard — never double-post for the same source record
        if (journalEntryRepository.existsByReferenceTypeAndReferenceId(referenceType, referenceId)) {
            log.info("GL entry already exists for {} #{}, skipping.", referenceType, referenceId);
            return journalEntryRepository
                    .findByReferenceTypeAndReferenceId(referenceType, referenceId)
                    .orElseThrow();
        }

        Account debitAccount  = accountRepository.findById(debitAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Debit account not found: " + debitAccountId));
        Account creditAccount = accountRepository.findById(creditAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Credit account not found: " + creditAccountId));

        JournalEntry entry = buildEntry(entryDate, description, referenceType, referenceId, schoolId);

        JournalLine debitLine = new JournalLine();
        debitLine.setAccount(debitAccount);
        debitLine.setDebitAmount(amount);
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setLineNumber(1);
        entry.addLine(debitLine);

        JournalLine creditLine = new JournalLine();
        creditLine.setAccount(creditAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(amount);
        creditLine.setLineNumber(2);
        entry.addLine(creditLine);

        postEntry(entry);
        JournalEntry saved = journalEntryRepository.save(entry);
        updateAccountBalances(saved);
        return saved;
    }

    @Override
    public JournalEntryResponseDTO reverseEntry(Long journalEntryId, String reason, Long schoolId) {
        JournalEntry original = findEntry(journalEntryId, schoolId);
        if (original.getStatus() != JournalEntryStatus.POSTED) {
            throw new IllegalStateException("Only POSTED entries can be reversed. Current status: " + original.getStatus());
        }

        // Create mirror entry with swapped debits/credits
        JournalEntry reversal = buildEntry(
                LocalDate.now(),
                "REVERSAL: " + original.getDescription() + (reason != null ? " | Reason: " + reason : ""),
                original.getReferenceType(),
                original.getReferenceId(),
                schoolId
        );
        reversal.setReversalOfEntryId(original.getId());

        int lineNum = 1;
        for (JournalLine originalLine : original.getLines()) {
            JournalLine reversalLine = new JournalLine();
            reversalLine.setAccount(originalLine.getAccount());
            // Swap debit ↔ credit
            reversalLine.setDebitAmount(originalLine.getCreditAmount());
            reversalLine.setCreditAmount(originalLine.getDebitAmount());
            reversalLine.setNarration("REVERSAL: " + (originalLine.getNarration() != null ? originalLine.getNarration() : ""));
            reversalLine.setLineNumber(lineNum++);
            reversal.addLine(reversalLine);
        }

        postEntry(reversal);
        journalEntryRepository.save(reversal);

        // Mark original as reversed
        original.setStatus(JournalEntryStatus.REVERSED);
        journalEntryRepository.save(original);

        // Undo original account balance updates
        undoAccountBalances(original);
        // Apply reversal balance updates
        updateAccountBalances(reversal);

        return toResponseDTO(reversal);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalEntryResponseDTO> getJournalEntries(
            Long schoolId, JournalEntryStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        return journalEntryRepository.findFiltered(schoolId, status, from, to, pageable)
                .map(this::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntryResponseDTO getJournalEntryById(Long id, Long schoolId) {
        return toResponseDTO(findEntry(id, schoolId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrialBalanceRowDTO> getTrialBalance(Long schoolId) {
        List<Account> allAccounts = accountRepository.findAll().stream()
                .filter(a -> schoolId.equals(a.getSchoolId()) && a.isPostingAccount())
                .collect(Collectors.toList());

        List<TrialBalanceRowDTO> rows = new ArrayList<>();
        for (Account account : allAccounts) {
            BigDecimal totalDebits  = journalLineRepository.sumDebitsByAccount(account.getId());
            BigDecimal totalCredits = journalLineRepository.sumCreditsByAccount(account.getId());
            if (totalDebits.compareTo(BigDecimal.ZERO) == 0 && totalCredits.compareTo(BigDecimal.ZERO) == 0) {
                continue; // Skip zero-balance accounts from trial balance display
            }

            boolean isDebitNormalBalance = account.getAccountType() == AccountType.ASSET
                    || account.getAccountType() == AccountType.EXPENSE;
            BigDecimal netBalance = isDebitNormalBalance
                    ? totalDebits.subtract(totalCredits)
                    : totalCredits.subtract(totalDebits);
            boolean isDebitBalance = netBalance.compareTo(BigDecimal.ZERO) >= 0 ?
                    isDebitNormalBalance : !isDebitNormalBalance;

            rows.add(new TrialBalanceRowDTO(
                    account.getId(),
                    account.getCode(),
                    account.getName(),
                    account.getAccountType(),
                    totalDebits,
                    totalCredits,
                    netBalance.abs(),
                    isDebitBalance
            ));
        }
        rows.sort((a, b) -> a.accountCode().compareTo(b.accountCode()));
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntryResponseDTO> getAccountLedger(Long accountId, LocalDate from, LocalDate to, Long schoolId) {
        List<JournalLine> lines = journalLineRepository.findLedgerLines(accountId, from, to);
        // Group lines back to their parent entries (distinct entries)
        return lines.stream()
                .map(jl -> jl.getJournalEntry())
                .distinct()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAccountBalanceAsOfDate(Long accountId, LocalDate asOfDate, Long schoolId) {
        // We get all lines posted to this account up to that date.
        // It sums debitAmount and creditAmount for finding net
        List<JournalLine> lines = journalLineRepository.findLedgerLines(accountId, LocalDate.of(2000, 1, 1), asOfDate); // or whatever beginning is
        BigDecimal totalDr = lines.stream().map(JournalLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCr = lines.stream().map(JournalLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Account acc = accountRepository.findById(accountId).orElseThrow();
        boolean isDebitNormal = acc.getAccountType() == AccountType.ASSET || acc.getAccountType() == AccountType.EXPENSE;
        
        if (isDebitNormal) {
            return totalDr.subtract(totalCr);
        } else {
            return totalCr.subtract(totalDr);
        }
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private JournalEntry buildEntry(LocalDate entryDate, String description,
                                    JournalReferenceType referenceType, Long referenceId, Long schoolId) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateEntryNumber(schoolId));
        entry.setEntryDate(entryDate);
        entry.setDescription(description);
        entry.setReferenceType(referenceType);
        entry.setReferenceId(referenceId);
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setSchoolId(schoolId);
        return entry;
    }

    private void postEntry(JournalEntry entry) {
        String currentUser = getCurrentUsername();
        entry.setStatus(JournalEntryStatus.POSTED);
        entry.setPostedBy(currentUser);
    }

    private void validateLines(List<JournalLineRequestDTO> lines) {
        if (lines == null || lines.size() < 2) {
            throw new IllegalArgumentException("A journal entry requires at least 2 lines.");
        }
        BigDecimal totalDebits  = lines.stream().map(l -> nvl(l.debitAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = lines.stream().map(l -> nvl(l.creditAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException(
                    String.format("Journal entry is out of balance. Debits: %s, Credits: %s", totalDebits, totalCredits)
            );
        }
        if (totalDebits.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Journal entry cannot have zero value.");
        }
    }

    private void updateAccountBalances(JournalEntry entry) {
        for (JournalLine line : entry.getLines()) {
            Account account = line.getAccount();
            boolean isDebitNormal = account.getAccountType() == AccountType.ASSET
                    || account.getAccountType() == AccountType.EXPENSE;
            BigDecimal delta = isDebitNormal
                    ? line.getDebitAmount().subtract(line.getCreditAmount())
                    : line.getCreditAmount().subtract(line.getDebitAmount());
            account.setBalance(account.getBalance().add(delta));
            accountRepository.save(account);
        }
    }

    private void undoAccountBalances(JournalEntry entry) {
        for (JournalLine line : entry.getLines()) {
            Account account = line.getAccount();
            boolean isDebitNormal = account.getAccountType() == AccountType.ASSET
                    || account.getAccountType() == AccountType.EXPENSE;
            BigDecimal delta = isDebitNormal
                    ? line.getDebitAmount().subtract(line.getCreditAmount())
                    : line.getCreditAmount().subtract(line.getDebitAmount());
            account.setBalance(account.getBalance().subtract(delta));
            accountRepository.save(account);
        }
    }

    private Account findActivePostingAccount(Long accountId, Long schoolId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));
        if (!schoolId.equals(account.getSchoolId())) throw new EntityNotFoundException("Account not found: " + accountId);
        if (!account.isActive()) throw new IllegalArgumentException("Account '" + account.getCode() + "' is inactive.");
        if (!account.isPostingAccount()) throw new IllegalArgumentException("Account '" + account.getCode() + "' is a group account and cannot receive journal entries.");
        return account;
    }

    private JournalEntry findEntry(Long id, Long schoolId) {
        JournalEntry je = journalEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Journal entry not found: " + id));
        if (!schoolId.equals(je.getSchoolId())) throw new EntityNotFoundException("Journal entry not found: " + id);
        return je;
    }

    private String generateEntryNumber(Long schoolId) {
        int year = Year.now().getValue();
        String prefix = "JE-" + year + "-";
        long count = journalEntryRepository.countByPrefix(schoolId, prefix);
        return prefix + String.format("%06d", count + 1);
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private JournalEntryResponseDTO toResponseDTO(JournalEntry entry) {
        List<JournalLine> lines = entry.getLines();
        BigDecimal totalDebits  = lines.stream().map(JournalLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = lines.stream().map(JournalLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<JournalLineResponseDTO> lineDTOs = lines.stream()
                .sorted((a, b) -> {
                    if (a.getLineNumber() == null || b.getLineNumber() == null) return 0;
                    return a.getLineNumber().compareTo(b.getLineNumber());
                })
                .map(l -> new JournalLineResponseDTO(
                        l.getLineId(),
                        l.getAccount().getId(),
                        l.getAccount().getCode(),
                        l.getAccount().getName(),
                        l.getDebitAmount(),
                        l.getCreditAmount(),
                        l.getNarration(),
                        l.getLineNumber()
                ))
                .collect(Collectors.toList());

        return new JournalEntryResponseDTO(
                entry.getId(),
                entry.getUuid(),
                entry.getEntryNumber(),
                entry.getEntryDate(),
                entry.getDescription(),
                entry.getReferenceType(),
                entry.getReferenceId(),
                entry.getStatus(),
                entry.getPostedBy(),
                entry.getReversalOfEntryId(),
                totalDebits,
                totalCredits,
                lineDTOs,
                entry.getCreatedAt(),
                entry.getCreatedBy()
        );
    }
}
