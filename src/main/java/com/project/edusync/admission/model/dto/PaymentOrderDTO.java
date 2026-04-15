package com.project.edusync.admission.model.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data
@Builder
public class PaymentOrderDTO {
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String keyId;
}
