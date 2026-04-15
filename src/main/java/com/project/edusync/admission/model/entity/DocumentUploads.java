package com.project.edusync.admission.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admission_documents")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUploads {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    private String birthCertificateUrl;
    private String studentPhotoUrl;
    private String parentPhotoUrl;
    private String aadhaarCardUrl;
    private String transferCertificateUrl;
    private String reportCardUrl;
    private String addressProofUrl;
    private String casteCertificateUrl;
    private String incomeCertificateUrl;
    private String medicalCertificateUrl;
}
