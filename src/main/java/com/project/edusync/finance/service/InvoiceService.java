package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.invoice.InvoiceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


/**
 * Service interface for managing Invoices.
 */
public interface InvoiceService {

    /**
     * Generates a new invoice on-demand for a single student.
     * This will find the student's fee structure and create an invoice
     * from all its particulars.
     *
     * @param studentId The ID of the student to invoice.
     * @return The response DTO of the newly created invoice.
     */
    InvoiceResponseDTO generateSingleInvoice(Long studentId);


    /**
     * Retrieves a paginated list of all invoices.
     *
     * @param pageable Pagination and sorting information.
     * @return A page of InvoiceResponseDTOs.
     */
    Page<InvoiceResponseDTO> getAllInvoices(Pageable pageable);

    /**
     * Retrieves a single invoice by its ID.
     *
     * @param invoiceId The ID of the invoice.
     * @return The response DTO of the found invoice.
     */
    InvoiceResponseDTO getInvoiceById(Long invoiceId);

    /**
     * Generates a PDF receipt for a fully paid invoice.
     *
     * @param invoiceId The ID of the invoice.
     * @return A byte array (byte[]) of the generated PDF.
     */
    byte[] getInvoiceReceipt(Long invoiceId);


    /**
     * Retrieves all invoices for a SPECIFIC student.
     * (Temporary method for testing parent logic without auth).
     *
     * @return A List of InvoiceResponseDTOs.
     */
    List<InvoiceResponseDTO> getInvoicesForStudent(Long studentId);


    /**
     * Retrieves a single invoice by its ID.
     * (Insecure: Does not check for parent ownership yet).
     *
     * @param invoiceId The ID of the invoice to retrieve.
     * @return The response DTO of the found invoice.
     */
    InvoiceResponseDTO getInvoiceByIdForParent(Long invoiceId);

    /**
     * Manually applies a late fee to an overdue invoice.
     *
     * @param invoiceId The ID of the invoice.
     * @return The updated invoice response DTO.
     */
    InvoiceResponseDTO applyLateFee(Long invoiceId);

    /**
     * Cancels an invoice (e.g., if issued in error).
     *
     * @param invoiceId The ID of the invoice to cancel.
     * @return The updated (cancelled) invoice response DTO.
     */
    InvoiceResponseDTO cancelInvoice(Long invoiceId);
    // We will add other methods like generate-bulk, get, etc., here later.
}