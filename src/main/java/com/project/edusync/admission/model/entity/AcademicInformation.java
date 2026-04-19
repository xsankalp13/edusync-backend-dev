package com.project.edusync.admission.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admission_academic_info")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcademicInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    private String previousSchoolName;
    private String board;
    private String lastClassAttended;
    private String marksOrGradeObtained;
    private String mediumOfInstruction;
    private String transferCertificateDetails;
}
