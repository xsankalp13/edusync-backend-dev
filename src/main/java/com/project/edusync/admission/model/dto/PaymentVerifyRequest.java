package com.project.edusync.admission.model.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;
@Data
public class PaymentVerifyRequest {
    @jakarta.validation.constraints.NotNull(message = "Application UUID is required")
    private UUID applicationUuid;
    @jakarta.validation.constraints.NotBlank(message = "Gateway Transaction ID is required")
    private String gatewayTransactionId;
    @jakarta.validation.constraints.NotBlank(message = "Order ID is required")
    private String orderId;
    @jakarta.validation.constraints.NotBlank(message = "Signature is required")
    private String signature;
}
