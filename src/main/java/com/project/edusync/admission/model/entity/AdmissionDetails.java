package com.project.edusync.admission.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admission_details")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdmissionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    @Column(nullable = false)
    private String classApplyingFor;

    @Column(nullable = false)
    private String academicYear;

    private String stream;
    private String secondLanguagePreference;
    private String thirdLanguagePreference;

    @Column(nullable = false)
    private boolean transportRequired;
    
    private boolean hostelRequired;
}
