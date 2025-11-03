package com.project.edusync.uis.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student extends AuditableEntity {

    // id, uuid, and audit fields are inherited.
    // 'id' here is the student_id.

    // All name/dob columns are REMOVED.

    @Column(name = "enrollment_number", length = 50, unique = true)
    private String enrollmentNumber;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    // This is the "Soft Delete" flag.
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // --- Relationships ---

    /**
     * This links the Student "Role" to the "Person" (UserProfile).
     * This is the new, correct relationship.
     * The 'profile_id' column must be unique, as one profile
     * can only be one student.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", referencedColumnName = "id", unique = true)
    private UserProfile userProfile;

    /**
     * Relationship to guardians. This remains the same.
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentGuardianRelationship> guardianRelationships = new HashSet<>();
}