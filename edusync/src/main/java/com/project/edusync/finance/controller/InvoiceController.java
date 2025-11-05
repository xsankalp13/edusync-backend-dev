package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.invoice.InvoiceResponseDTO;
import com.project.edusync.finance.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.url}/auth/finance/invoices") // Base path: /api/v1/finance/invoices
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * POST /api/v1/finance/invoices/generate-single/{studentId}
     * Generates a new invoice on-demand for a single student.
     */
    @PostMapping("/generate-single/{studentId}")
    public ResponseEntity<InvoiceResponseDTO> generateSingleInvoice(
            @PathVariable Long studentId) {

        InvoiceResponseDTO response = invoiceService.generateSingleInvoice(studentId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/finance/invoices
     * Retrieves a paginated list of all student invoices.
     * Spring Boot will automatically inject Pageable from query params:
     * e.g., ?page=0&size=10&sort=issueDate,desc
     */
    @GetMapping
    public ResponseEntity<Page<InvoiceResponseDTO>> getAllInvoices(Pageable pageable) {

        Page<InvoiceResponseDTO> pageResponse = invoiceService.getAllInvoices(pageable);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }

    /**
     * GET /api/v1/finance/invoices/{invoiceId}
     * Retrieves the complete detail of a single invoice, including line items.
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponseDTO> getInvoiceById(@PathVariable Long invoiceId) {

        InvoiceResponseDTO response = invoiceService.getInvoiceById(invoiceId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * GET /api/v1/finance/invoices/{invoiceId}/receipt
     * Downloads a PDF receipt for a fully paid invoice.
     */
    @GetMapping("/{invoiceId}/receipt")
    public ResponseEntity<byte[]> getInvoiceReceipt(@PathVariable Long invoiceId) {

        byte[] pdfBytes = invoiceService.getInvoiceReceipt(invoiceId);

        String filename = "receipt-" + invoiceId + ".pdf";

        // Set headers to trigger browser download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

}