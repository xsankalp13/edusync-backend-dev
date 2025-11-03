package com.project.edusync.uis.model.entity.medical;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_medical_medications")
@Getter
@Setter
@NoArgsConstructor
public class StudentMedicalMedication extends AuditableEntity {

    // id (as medication_id), uuid, and audit fields are inherited.

    @Column(name = "medication_name", length = 100, nullable = false)
    private String medicationName;

    @Column(length = 50)
    private String dosage;

    @Column(length = 100)
    private String frequency;

    @Column(name = "is_school_administered", nullable = false)
    private boolean isSchoolAdministered = false;

    @Column(name = "administration_notes", columnDefinition = "TEXT")
    private String administrationNotes;

    // --- Relationships ---

    /**
     * This is the link back to the parent medical record.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id")
    private StudentMedicalRecord medicalRecord;
}
