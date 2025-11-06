package com.project.edusync.finance.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Used for POST /payments/verify (Parent API) [cite: 21]
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentRequestDTO {
    @NotNull
    private String gatewayTransactionId;
    @NotNull
    private String orderId;
    /**
     * The cryptographic signature returned by Razorpay on success.
     */
    @NotNull
    private String signature;
}
