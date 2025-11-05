package com.project.edusync.em.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity for examination.grade_systems table.
 * A high-level grading system (e.g., "CBSE 9-Point").
 */
@Entity
@Table(name = "grade_systems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "grade_system_id"))
public class GradeSystem extends AuditableEntity {

    @Column(name = "system_name", nullable = false, unique = true, length = 100)
    private String systemName;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // --- Relationships ---

    /**
     * A GradeSystem has many GradeScales (A1, A2, B1, etc.).
     * cascade = CascadeType.ALL: If you delete the system, delete all its scales.
     * orphanRemoval = true: If you remove a scale from this set, delete it from the DB.
     */
    @OneToMany(
            mappedBy = "gradeSystem",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private Set<GradeScale> gradeScales = new HashSet<>();
}


