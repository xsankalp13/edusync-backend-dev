package com.project.edusync.admission.model.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class EnquiryReplyRequest {
    @NotBlank(message = "Reply message is required")
    private String reply;
}
