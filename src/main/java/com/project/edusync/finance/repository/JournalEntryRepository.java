package com.project.edusync.finance.repository;

import com.project.edusync.finance.model.entity.JournalEntry;
import com.project.edusync.finance.model.enums.JournalEntryStatus;
import com.project.edusync.finance.model.enums.JournalReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    /** Find the entry linked to a specific source transaction — e.g. find GL entry for Payment #123. */
    Optional<JournalEntry> findByReferenceTypeAndReferenceId(JournalReferenceType referenceType, Long referenceId);

    /** Check if an auto-entry was already posted for a source (idempotency guard). */
    boolean existsByReferenceTypeAndReferenceId(JournalReferenceType referenceType, Long referenceId);

    /** Paginated, school-scoped listing with optional date and status filters. */
    @Query("""
        SELECT je FROM JournalEntry je
        WHERE je.schoolId = :schoolId
          AND (je.status = :status OR CAST(:status AS String) IS NULL)
          AND (je.entryDate >= :from OR CAST(:from AS LocalDate) IS NULL)
          AND (je.entryDate <= :to OR CAST(:to AS LocalDate) IS NULL)
        ORDER BY je.entryDate DESC, je.entryNumber DESC
    """)
    Page<JournalEntry> findFiltered(
        @Param("schoolId") Long schoolId,
        @Param("status") JournalEntryStatus status,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        Pageable pageable
    );

    /** All posted entries within a date range — used for trial balance and financial statements. */
    @Query("""
        SELECT je FROM JournalEntry je
        WHERE je.schoolId = :schoolId
          AND je.status = 'POSTED'
          AND je.entryDate BETWEEN :from AND :to
    """)
    List<JournalEntry> findPostedInRange(
        @Param("schoolId") Long schoolId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /** Generate the next sequential entry number for a school+year prefix. */
    @Query("SELECT COUNT(je) FROM JournalEntry je WHERE je.schoolId = :schoolId AND je.entryNumber LIKE :prefix%")
    Long countByPrefix(@Param("schoolId") Long schoolId, @Param("prefix") String prefix);

    /**
     * Find POSTED GL entries on a given date where any line equals the given amount.
     * Used by the bank reconciliation auto-match engine.
     */
    @Query("""
        SELECT DISTINCT je FROM JournalEntry je
        JOIN je.lines jl
        WHERE je.schoolId = :schoolId
          AND je.status = 'POSTED'
          AND je.entryDate = :date
          AND (jl.debitAmount = :amount OR jl.creditAmount = :amount)
        ORDER BY je.entryDate DESC
    """)
    List<JournalEntry> findByDateAndAmount(
        @Param("date") java.time.LocalDate date,
        @Param("amount") java.math.BigDecimal amount,
        @Param("schoolId") Long schoolId
    );
}
