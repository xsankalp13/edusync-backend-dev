package com.project.edusync.adm.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a specific section of a class (e.g., "Grade 10 - Section A").
 *
 * This entity extends AuditableEntity to gain ID (Long), UUID,
 * and audit timestamp fields.
 *
 * Relationships will be joined using the inherited 'id' (Long) primary key.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"academicClass", "defaultRoom", "schedules", "academicConstraints", "students"})
@ToString(callSuper = true, exclude = {"academicClass", "defaultRoom", "schedules", "academicConstraints", "students"})
@Entity
@Table(name = "sections")
public class Section extends AuditableEntity {

    // The @Id (Long id) and 'uuid' are inherited from AuditableEntity.

    @Column(name = "section_name", nullable = false, length = 100)
    private String sectionName;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // --- Relationships ---

    /**
     * The class this section belongs to (e.g., "Grade 10").
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false) // This joins on the 'id' column of the 'classes' table
    private AcademicClass academicClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_room_id")
    private Room defaultRoom;

    /**
     * All schedule entries for this specific section.
     */
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Schedule> schedules = new HashSet<>();

    /**
     * All constraints that apply to this specific section.
     */
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<AcademicConstraint> academicConstraints = new HashSet<>();

    /**
     * All students enrolled in this specific section.
     * This is the "One-to-Many" (inverse) side of the relationship.
     */
    @OneToMany(mappedBy = "section", fetch = FetchType.LAZY)
    private Set<Student> students = new HashSet<>();

}
