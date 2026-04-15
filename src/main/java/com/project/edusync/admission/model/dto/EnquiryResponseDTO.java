package com.project.edusync.admission.model.dto;
import com.project.edusync.admission.model.enums.EnquiryStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
public class EnquiryResponseDTO {
    private UUID uuid;
    private String applicantName;
    private String applicantEmail;
    private String applicantMobile;
    private String subject;
    private String message;
    private String adminReply;
    private String adminRepliedBy;
    private LocalDateTime adminRepliedAt;
    private String classApplyingFor;
    private String academicYear;
    private EnquiryStatus status;
    private LocalDateTime createdAt;
}
