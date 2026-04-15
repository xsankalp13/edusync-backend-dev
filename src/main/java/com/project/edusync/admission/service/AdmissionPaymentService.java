package com.project.edusync.admission.service;

import com.project.edusync.finance.dto.payment.InitiatePaymentResponseDTO;
import com.project.edusync.admission.model.dto.PaymentVerifyRequest;
import com.project.edusync.iam.model.entity.User;

import java.util.UUID;

public interface AdmissionPaymentService {
    InitiatePaymentResponseDTO createOrder(UUID applicationUuid, User user);
    void verifyPayment(PaymentVerifyRequest request, User user);
    byte[] getAdmissionReceipt(UUID applicationUuid, User user);
}
