package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.invoice.InvoiceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


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
     * NEW METHOD: Retrieves a paginated list of all invoices.
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
    // We will add other methods like generate-bulk, get, etc., here later.
}