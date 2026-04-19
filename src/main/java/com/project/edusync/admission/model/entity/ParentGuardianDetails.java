package com.project.edusync.admission.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admission_parent_details")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParentGuardianDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    // Father Details
    @Column(nullable = false)
    private String fatherName;
    private String fatherOccupation;
    private String fatherQualification;
    private String fatherAnnualIncome;
    @Column(nullable = false)
    private String fatherMobile;

    // Mother Details
    @Column(nullable = false)
    private String motherName;
    private String motherOccupation;
    private String motherQualification;
    private String motherAnnualIncome;
    private String motherMobile;

    // Guardian Details
    private String guardianName;
    private String guardianRelationship;
    private String guardianContact;
}
