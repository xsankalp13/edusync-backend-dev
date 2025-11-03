package com.project.edusync.uis.model.entity.medical;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_medical_records")
@Getter
@Setter
@NoArgsConstructor
public class StudentMedicalRecord extends AuditableEntity {

    // id (as record_id), uuid, and audit fields are inherited.

    @Column(name = "primary_care_physician", length = 100)
    private String primaryCarePhysician;

    @Column(name = "physician_phone", length = 20)
    private String physicianPhone;

    @Column(name = "insurance_provider", length = 100)
    private String insuranceProvider;

    @Column(name = "insurance_policy_number", length = 50)
    private String insurancePolicyNumber;

    // --- Relationships ---

    /**
     * This is the link to the Student.
     * The @JoinColumn has 'unique = true' to enforce the
     * one-to-one relationship as specified in your schema.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", referencedColumnName = "id", unique = true)
    private Student student;

    /**
     * A medical record can have many allergies.
     * 'mappedBy = "medicalRecord"' refers to the field in StudentMedicalAllergy.
     */
    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentMedicalAllergy> allergies = new HashSet<>();

    /**
     * A medical record can have many medications.
     */
    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentMedicalMedication> medications = new HashSet<>();
}
