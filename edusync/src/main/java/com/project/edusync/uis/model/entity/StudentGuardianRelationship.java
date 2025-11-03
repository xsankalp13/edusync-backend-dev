package com.project.edusync.uis.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_guardian_relationships",
        uniqueConstraints = {
                // This constraint ensures a student-guardian pair is unique.
                @UniqueConstraint(columnNames = {"student_id", "guardian_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StudentGuardianRelationship extends AuditableEntity {

    // id, uuid, createdAt, updatedAt, createdBy, updatedBy
    // are all INHERITED from AuditableEntity.

    // --- Relationships ---

    /**
     * The student in this relationship.
     * We use FetchType.LAZY to avoid loading the full Student object
     * unless we explicitly ask for it.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    /**
     * The guardian in this relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id")
    private Guardian guardian;

    // --- Payload Fields ---

    /**
     * The nature of the relationship, e.g., "Mother", "Father", "Legal Guardian".
     */
    @Column(name = "relationship_type", length = 50, nullable = false)
    private String relationshipType;

    /**
     * Flag to identify the main point of contact for the student.
     * Your "Truancy Alerts" feature will use this field.
     */
    @Column(name = "is_primary_contact", nullable = false)
    private boolean isPrimaryContact = false;

    @Column(name = "can_pickup", nullable = false)
    private boolean canPickup = false;

    @Column(name = "is_financial_contact", nullable = false)
    private boolean isFinancialContact = false;

    @Column(name = "can_view_grades", nullable = false)
    private boolean canViewGrades = false;

}