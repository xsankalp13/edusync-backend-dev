package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.payment.PaymentResponseDTO;
import com.project.edusync.finance.dto.payment.PaymentUpdateDTO;
import com.project.edusync.finance.dto.payment.RecordOfflinePaymentDTO;
import com.project.edusync.finance.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/finance/payments") // Base path: /api/v1/finance/payments
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/finance/payments/record-offline
     * Manually records a payment (Cash, Check) against an invoice.
     */
    @PostMapping("/record-offline")
    public ResponseEntity<PaymentResponseDTO> recordOfflinePayment(
            @Valid @RequestBody RecordOfflinePaymentDTO createDTO) {

        PaymentResponseDTO response = paymentService.recordOfflinePayment(createDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/finance/payments
     * Retrieves a paginated list of all payment transactions.
     * e.g., ?page=0&size=10&sort=paymentDate,desc
     */
    @GetMapping
    public ResponseEntity<Page<PaymentResponseDTO>> getAllPayments(Pageable pageable) {
        Page<PaymentResponseDTO> pageResponse = paymentService.getAllPayments(pageable);
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);
    }


    /**
     * GET /api/v1/finance/payments/{paymentId}
     * Retrieves details for a single payment transaction.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(@PathVariable Long paymentId) {
        PaymentResponseDTO response = paymentService.getPaymentById(paymentId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * PUT /api/v1/finance/payments/{paymentId}
     * Updates an existing payment record (e.g., to add notes, update check status).
     */
    @PutMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> updatePayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentUpdateDTO updateDTO) {

        PaymentResponseDTO response = paymentService.updatePayment(paymentId, updateDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * GET /api/v1/auth/finance/payments/invoice/{invoiceId}
     * Retrieves all payments associated with a specific invoice.
     */
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByInvoiceId(@PathVariable Long invoiceId) {
        List<PaymentResponseDTO> payments = paymentService.getPaymentsByInvoiceId(invoiceId);
        return new ResponseEntity<>(payments, HttpStatus.OK);
    }

    /**
     * GET /api/v1/auth/finance/payments/{paymentId}/receipt
     * Generates a PDF receipt for a specific payment transaction.
     */
    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<byte[]> getPaymentReceipt(@PathVariable Long paymentId) {
        byte[] pdf = paymentService.getPaymentReceipt(paymentId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=receipt_" + paymentId + ".pdf")
                .body(pdf);
    }
}
