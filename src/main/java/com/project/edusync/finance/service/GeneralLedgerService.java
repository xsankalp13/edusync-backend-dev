package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.gl.JournalEntryRequestDTO;
import com.project.edusync.finance.dto.gl.JournalEntryResponseDTO;
import com.project.edusync.finance.dto.gl.TrialBalanceRowDTO;
import com.project.edusync.finance.model.entity.JournalEntry;
import com.project.edusync.finance.model.enums.JournalEntryStatus;
import com.project.edusync.finance.model.enums.JournalReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GeneralLedgerService {

    /**
     * Creates and immediately POSTS a manual journal entry from the UI.
     * Validates: debits == credits, accounts are active posting accounts.
     */
    JournalEntryResponseDTO createAndPostManualEntry(JournalEntryRequestDTO dto, Long schoolId);

    /**
     * Auto-creates and posts a system-generated double-entry record.
     * Used internally by PaymentServiceImpl, PayrollServiceImpl, etc.
     * Idempotent: if an entry already exists for (referenceType, referenceId), returns it.
     *
     * @param entryDate      The accounting date
     * @param description    Narration for the ledger
     * @param referenceType  The source module
     * @param referenceId    The source record ID
     * @param debitAccountId Account to debit
     * @param creditAccountId Account to credit
     * @param amount         Amount
     * @param schoolId       School scope
     */
    JournalEntry autoPostEntry(
        LocalDate entryDate,
        String description,
        JournalReferenceType referenceType,
        Long referenceId,
        Long debitAccountId,
        Long creditAccountId,
        BigDecimal amount,
        Long schoolId
    );

    /**
     * Reverses a POSTED journal entry by creating a mirror contra-entry.
     * The original entry is marked REVERSED; the new entry is POSTED.
     */
    JournalEntryResponseDTO reverseEntry(Long journalEntryId, String reason, Long schoolId);

    /** Paginated, filtered list of journal entries for the GL screen. */
    Page<JournalEntryResponseDTO> getJournalEntries(
        Long schoolId,
        JournalEntryStatus status,
        LocalDate from,
        LocalDate to,
        Pageable pageable
    );

    /** Get a single journal entry by ID with all its lines. */
    JournalEntryResponseDTO getJournalEntryById(Long id, Long schoolId);

    /**
     * Returns the Trial Balance — every active account with its total debits,
     * total credits, and net balance. Only includes accounts with non-zero totals.
     */
    List<TrialBalanceRowDTO> getTrialBalance(Long schoolId);

    /**
     * Returns the detailed ledger for a specific account:
     * all journal lines posted to that account in the date range,
     * in chronological order with a running balance.
     */
    List<JournalEntryResponseDTO> getAccountLedger(Long accountId, LocalDate from, LocalDate to, Long schoolId);

    /** Create Entry with multiple lines */
    JournalEntry createJournalEntry(
        LocalDate entryDate,
        String description,
        JournalReferenceType referenceType,
        Long referenceId,
        List<com.project.edusync.finance.model.entity.JournalLine> lines,
        Long schoolId
    );

    /** Get account balance as of specific date */
    BigDecimal getAccountBalanceAsOfDate(Long accountId, LocalDate asOfDate, Long schoolId);
}
