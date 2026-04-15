package com.project.edusync.admission.model.dto;
import com.project.edusync.admission.model.enums.AdmissionStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
public class ApplicationSummaryDTO {
    private Long id;
    private UUID uuid;
    private String applicantName;
    private String email;
    private AdmissionStatus status;
    private String classApplyingFor;
    private int currentSection;
    private LocalDateTime createdAt;
    private BigDecimal formFee;
}
