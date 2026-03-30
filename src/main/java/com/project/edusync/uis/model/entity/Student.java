package com.project.edusync.uis.model.entity;

import com.project.edusync.adm.model.entity.Section;
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


    @Column(name = "enrollment_number", length = 50, unique = true)
    private String enrollmentNumber;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "expected_graduation_year")
    private Integer expectedGraduationYear;

    @Column(name = "counselor_name", length = 120)
    private String counselorName;

    @Column(name = "roll_no", nullable = false)
    private Integer rollNo;

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

    /**
     * The section this student is currently enrolled in.
     * This is the "Many-to-One" side and owns the relationship.
     * The 'students' table will have a 'section_id' foreign key.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;
}