package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.Invoice;
import com.project.edusync.finance.model.enums.InvoiceStatus;
import com.project.edusync.uis.model.entity.Student;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Invoice} entity.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Finds an invoice by its unique, human-readable invoice number.
     *
     * @param invoiceNumber The invoice number (e.g., "INV-2025-0001").
     * @return An Optional containing the found Invoice, or empty if not found.
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Fetches an invoice and immediately acquires a pessimistic write lock
     * (SELECT ... FOR UPDATE) on the row. This prevents concurrent transactions
     * from reading stale paidAmount values and overwriting each other's updates.
     *
     * @param id The invoice primary key.
     * @return An Optional containing the locked Invoice, or empty if not found.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findByIdWithLock(@Param("id") Long id);

    /**
     * Checks whether a student already has at least one invoice whose status
     * is NOT in the supplied exclusion set.  Used to guard against generating
     * a second "open" invoice for the same student.
     *
     * @param student   The Student entity.
     * @param statuses  Statuses to EXCLUDE from the check (typically CANCELLED).
     * @return {@code true} if an active invoice already exists.
     */
    boolean existsByStudentAndStatusNotIn(Student student, Collection<InvoiceStatus> statuses);

    /**
     * Finds all invoices for a specific student.
     *
     * @param student The Student entity.
     * @return A list of invoices for that student.
     */
    List<Invoice> findByStudent(Student student);

    /**
     * Finds all invoices that have a specific status.
     *
     * @param status The InvoiceStatus enum (e.g., PENDING, OVERDUE).
     * @return A list of invoices matching that status.
     */
    List<Invoice> findByStatus(InvoiceStatus status);

    /**
     * Finds all invoices for a specific student with a specific status.
     *
     * @param student The Student entity.
     * @param status The InvoiceStatus enum.
     * @return A list of matching invoices.
     */
    List<Invoice> findByStudentAndStatus(Student student, InvoiceStatus status);

    /**
     * Calculates the total outstanding amount from all PENDING or OVERDUE invoices.
     */
    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) " +
            "FROM Invoice i " +
            "WHERE i.status = 'PENDING' OR i.status = 'OVERDUE'")
    BigDecimal findTotalOutstanding();

    /**
     * Calculates the total overdue amount from OVERDUE invoices only.
     */
    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) " +
            "FROM Invoice i " +
            "WHERE i.status = 'OVERDUE'")
    BigDecimal findTotalOverdue();

    /**
     * Counts the number of invoices with a PENDING status.
     */
    @Query("SELECT COUNT(i) " +
            "FROM Invoice i " +
            "WHERE i.status = 'PENDING'")
    Long countPendingInvoices();

    /**
     * Calculates the total outstanding amount for a single student.
     */
    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) " +
            "FROM Invoice i " +
            "WHERE (i.status = 'PENDING' OR i.status = 'OVERDUE') " +
            "AND i.student.id = :studentId")
    BigDecimal findTotalDueForStudent(@Param("studentId") Long studentId);

    /**
     * Finds the earliest due date for all pending/overdue invoices for a single student.
     */
    @Query("SELECT MIN(i.dueDate) " +
            "FROM Invoice i " +
            "WHERE (i.status = 'PENDING' OR i.status = 'OVERDUE') " +
            "AND i.student.id = :studentId")
    Optional<LocalDate> findNextDueDateForStudent(@Param("studentId") Long studentId);

    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) " +
            "FROM Invoice i " +
            "WHERE i.status = 'OVERDUE' " +
            "AND i.student.id = :studentId")
    BigDecimal findTotalOverdueForStudent(@Param("studentId") Long studentId);

    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0)
            FROM Invoice i
            WHERE YEAR(i.issueDate) = :year
              AND MONTH(i.issueDate) = :month
              AND i.status != 'CANCELLED'
            """)
    BigDecimal sumExpectedByIssueYearMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Projection for monthly expected invoice totals.
     */
    interface MonthlyInvoiceSumProjection {
        Integer getInvoiceYear();
        Integer getInvoiceMonth();
        java.math.BigDecimal getExpectedTotal();
    }

    /**
     * Returns (invoiceYear, invoiceMonth, expectedTotal) for each month in the range.
     * Replaces 6 individual sumExpectedByIssueYearMonth calls in MasterDashboardAnalyticsServiceImpl.
     * 1 query instead of 6.
     */
    @Query("""
            SELECT YEAR(i.issueDate) as invoiceYear,
                   MONTH(i.issueDate) as invoiceMonth,
                   COALESCE(SUM(i.totalAmount), 0) as expectedTotal
            FROM Invoice i
            WHERE i.issueDate >= :startDate
              AND i.issueDate <= :endDate
              AND i.status != 'CANCELLED'
            GROUP BY YEAR(i.issueDate), MONTH(i.issueDate)
            ORDER BY YEAR(i.issueDate), MONTH(i.issueDate)
            """)
    java.util.List<MonthlyInvoiceSumProjection> sumExpectedGroupedByMonth(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    /**
     * Returns total expected invoice amounts issued in an arbitrary date range (for MTD comparisons).
     */
    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0)
            FROM Invoice i
            WHERE i.issueDate >= :startDate
              AND i.issueDate <= :endDate
              AND i.status != 'CANCELLED'
            """)
    java.math.BigDecimal sumExpectedByDateRange(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );
}
