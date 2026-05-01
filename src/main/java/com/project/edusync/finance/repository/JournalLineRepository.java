package com.project.edusync.finance.repository;

import com.project.edusync.finance.model.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    /** All lines for a journal entry — used to validate debit/credit balance. */
    List<JournalLine> findByJournalEntryIdOrderByLineNumber(Long journalEntryId);

    /**
     * Compute the account ledger for a given account and date range (posted entries only).
     * Returns lines ordered chronologically for display in the Ledger tab.
     */
    @Query("""
        SELECT jl FROM JournalLine jl
        JOIN jl.journalEntry je
        WHERE jl.account.id = :accountId
          AND je.status = 'POSTED'
          AND (je.entryDate >= :from OR CAST(:from AS LocalDate) IS NULL)
          AND (je.entryDate <= :to OR CAST(:to AS LocalDate) IS NULL)
        ORDER BY je.entryDate ASC, je.entryNumber ASC
    """)
    List<JournalLine> findLedgerLines(
        @Param("accountId") Long accountId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Calculates the total debit balance for an account across all posted entries.
     * Used for trial balance computation.
     */
    @Query("""
        SELECT COALESCE(SUM(jl.debitAmount), 0) FROM JournalLine jl
        JOIN jl.journalEntry je
        WHERE jl.account.id = :accountId AND je.status = 'POSTED'
    """)
    BigDecimal sumDebitsByAccount(@Param("accountId") Long accountId);

    /**
     * Calculates the total credit balance for an account across all posted entries.
     */
    @Query("""
        SELECT COALESCE(SUM(jl.creditAmount), 0) FROM JournalLine jl
        JOIN jl.journalEntry je
        WHERE jl.account.id = :accountId AND je.status = 'POSTED'
    """)
    BigDecimal sumCreditsByAccount(@Param("accountId") Long accountId);
}
