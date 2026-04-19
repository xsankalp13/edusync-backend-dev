package com.project.edusync.admission.model.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class EnquiryCreateRequest {
    @NotBlank(message = "Subject is required")
    private String subject;
    @NotBlank(message = "Message is required")
    private String message;
    private String classApplyingFor;
    private String academicYear;
}
