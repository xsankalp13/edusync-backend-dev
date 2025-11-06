package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.Invoice;
import com.project.edusync.finance.model.enums.InvoiceStatus;
import com.project.edusync.uis.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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
}
