package com.project.edusync.finance.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Used for POST /payments/initiate (Parent API) [cite: 21]
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequestDTO {
    @NotNull
    private Long invoiceId;
    /**
     * The amount the parent wishes to pay (supports partial payments).
     */
    @NotNull
    @Positive
    private BigDecimal amount;
}