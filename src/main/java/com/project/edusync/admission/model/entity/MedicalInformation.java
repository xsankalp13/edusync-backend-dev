package com.project.edusync.admission.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admission_medical_info")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MedicalInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    private String allergies;
    private String existingMedicalConditions;
    private String disabilities;
    private String emergencyContactPerson;
    private String emergencyContactNumber;
}
