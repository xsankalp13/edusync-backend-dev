package com.project.edusync.admission.model.entity;

import com.project.edusync.admission.model.enums.AdmissionStatus;
import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.iam.model.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "admission_applications")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdmissionApplication extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AdmissionStatus status = AdmissionStatus.DRAFT;

    @Builder.Default
    private int currentSection = 1;

    @Column(columnDefinition = "TEXT")
    private String adminRemarks;

    private BigDecimal formFee;

    private String feePaymentId;

    private String razorpayOrderId;

    private String approvedBy;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime paidAt;

    // Relationships to sections will be added as @OneToOne on the section entities side or here.
    // Given the multi-step nature, we'll keep them on the master entity for easy retrieval in DetailDTO.

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private StudentBasicDetails studentBasicDetails;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AddressContactDetails addressContactDetails;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ParentGuardianDetails parentGuardianDetails;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AcademicInformation academicInformation;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DocumentUploads documentUploads;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AdmissionDetails admissionDetails;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MedicalInformation medicalInformation;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TransportDetails transportDetails;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DeclarationSection declarationSection;
}
