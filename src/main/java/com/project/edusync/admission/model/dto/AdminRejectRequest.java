package com.project.edusync.admission.model.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class AdminRejectRequest {
    @NotBlank(message = "Remarks are required for rejection")
    private String remarks;
}
