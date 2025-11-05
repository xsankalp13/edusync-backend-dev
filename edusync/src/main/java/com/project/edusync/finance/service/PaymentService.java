package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.payment.PaymentResponseDTO;
import com.project.edusync.finance.dto.payment.RecordOfflinePaymentDTO;

/**
 * Service interface for managing Payments.
 */
public interface PaymentService {

    /**
     * Records an offline payment (Cash, Check) against an invoice.
     * This will create a new Payment record and update the Invoice's status.
     *
     * @param createDTO The DTO containing payment details.
     * @return The response DTO of the newly created payment.
     */
    PaymentResponseDTO recordOfflinePayment(RecordOfflinePaymentDTO createDTO);

    // We will add get, list, and update methods later.
}