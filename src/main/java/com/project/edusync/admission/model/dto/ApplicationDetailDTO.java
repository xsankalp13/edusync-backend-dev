package com.project.edusync.admission.model.dto;
import com.project.edusync.admission.model.enums.AdmissionStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
public class ApplicationDetailDTO {
    private UUID uuid;
    private AdmissionStatus status;
    private int currentSection;
    private String adminRemarks;
    private BigDecimal formFee;
    private String feePaymentId;
    private LocalDateTime submittedAt;
    
    private StudentBasicDetailsDTO studentBasicDetails;
    private AddressContactDTO addressContactDetails;
    private ParentGuardianDTO parentGuardianDetails;
    private AcademicInfoDTO academicInformation;
    private DocumentUploadsDTO documentUploads;
    private AdmissionDetailsDTO admissionDetails;
    private MedicalInfoDTO medicalInformation;
    private TransportDetailsDTO transportDetails;
    private DeclarationDTO declarationSection;
}
