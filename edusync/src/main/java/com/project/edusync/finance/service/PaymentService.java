package com.project.edusync.finance.service;

import com.project.edusync.common.exception.finance.InvoiceAlreadyPaidException;
import com.project.edusync.finance.dto.payment.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    /**
     * Retrieves a paginated list of all payment transactions.
     *
     * @param pageable Pagination and sorting information.
     * @return A page of PaymentResponseDTOs.
     */
    Page<PaymentResponseDTO> getAllPayments(Pageable pageable);


    /**
     * Retrieves details for a single payment transaction.
     *
     * @param paymentId The ID of the payment.
     * @return The response DTO of the found payment.
     */
    PaymentResponseDTO getPaymentById(Long paymentId);

    /**
     * Updates an existing payment record.
     *
     * @param paymentId The ID of the payment to update.
     * @param updateDTO The DTO with new data.
     * @return The response DTO of the updated payment.
     */
    PaymentResponseDTO updatePayment(Long paymentId, PaymentUpdateDTO updateDTO);

    /**
     * Initiates an online payment for an invoice.
     * Creates a 'PENDING' payment record and a Razorpay Order.
     *
     * @param requestDTO The DTO containing invoiceId and amount.
     * @return A DTO with the order_id and key_id for the frontend.
     */
    InitiatePaymentResponseDTO initiateOnlinePayment(InitiatePaymentRequestDTO requestDTO) throws Exception;


    /**
     * Verifies a completed Razorpay payment using the cryptographic signature.
     * If successful, updates Payment and Invoice status to 'PAID' or 'PENDING' (partial).
     *
     * @param verifyDTO The DTO from the client containing Razorpay's response.
     * @return The response DTO of the confirmed, 'SUCCESS' payment.
     */
    PaymentResponseDTO verifyOnlinePayment(VerifyPaymentRequestDTO verifyDTO) throws Exception;
}