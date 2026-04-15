package com.project.edusync.admission.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "admission_declaration")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeclarationSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    @Column(nullable = false)
    private boolean informationCorrect;

    @Column(nullable = false)
    private boolean agreesToRules;

    private String signatureUrl;

    @Column(nullable = false)
    private LocalDate declarationDate;
}
