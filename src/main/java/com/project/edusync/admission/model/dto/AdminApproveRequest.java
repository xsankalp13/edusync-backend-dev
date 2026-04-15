package com.project.edusync.admission.model.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class AdminApproveRequest {
    @NotNull(message = "Form fee is required")
    private BigDecimal formFee;
    private String remarks;
}
