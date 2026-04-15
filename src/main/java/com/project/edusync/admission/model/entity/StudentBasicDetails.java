package com.project.edusync.admission.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "admission_student_basic")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentBasicDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String gender;

    private String bloodGroup;

    @Column(nullable = false)
    private String nationality;

    private String religion;

    private String caste;

    private String aadhaarNumber;

    @Column(nullable = false)
    private String motherTongue;

    @Column(nullable = false)
    private String category;
}
