package com.project.edusync.uis.model.entity.medical;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.enums.AllergySeverity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_medical_allergies")
@Getter
@Setter
@NoArgsConstructor
public class StudentMedicalAllergy extends AuditableEntity {

    // id (as allergy_id), uuid, and audit fields are inherited.

    @Column(name = "allergy_name", length = 100, nullable = false)
    private String allergyName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AllergySeverity severity;

    @Column(name = "reaction_details", columnDefinition = "TEXT")
    private String reactionDetails;

    @Column(name = "is_life_threatening", nullable = false)
    private boolean isLifeThreatening = false;

    // --- Relationships ---

    /**
     * This is the link back to the parent medical record.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id")
    private StudentMedicalRecord medicalRecord;
}
