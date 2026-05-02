package com.project.edusync.finance.dto.payment;

// Used for GET /payments and GET /payments/{paymentId}


import com.project.edusync.finance.model.enums.PaymentMethod;
import com.project.edusync.finance.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private Long paymentId;
    private Long invoiceId;
    private Long studentId;
    private String studentName;
    private String transactionId;
    private LocalDateTime paymentDate;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod; // 'ONLINE', 'CASH', 'CHECK'
    private PaymentStatus status; // 'SUCCESS', 'PENDING', 'FAILED'
    private String notes;
    // Getters and Setters
}
