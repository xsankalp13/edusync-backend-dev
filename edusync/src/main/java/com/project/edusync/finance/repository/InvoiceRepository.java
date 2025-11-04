package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.Invoice;
import com.project.edusync.finance.model.enums.InvoiceStatus;
import com.project.edusync.uis.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
