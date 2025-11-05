package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.payment.PaymentResponseDTO;
import com.project.edusync.finance.dto.payment.RecordOfflinePaymentDTO;
import com.project.edusync.finance.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.url}//auth/finance/payments") // Base path: /api/v1/finance/payments
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
}
