package com.project.edusync.uis.model.entity;

import com.project.edusync.uis.model.enums.StaffType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
public class Staff extends com.project.edusync.common.model.AuditableEntity {

    // id, uuid, and audit fields are inherited.
    // 'id' here is the staff_id.

    @Column(name = "job_title", length = 100, nullable = false)
    private String jobTitle; // e.g., "History Teacher", "Librarian"

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", length = 50, nullable = false)
    private StaffType staffType; // e.g., "TEACHER", "LIBRARIAN", "PRINCIPAL"

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "office_location", length = 50)
    private String officeLocation;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // --- Relationships ---

    /**
     * This links the Staff "Role" to the "Person" (UserProfile).
     * The 'profile_id' column must be unique.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", referencedColumnName = "id", unique = true)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_staff_id")
    private Staff manager;
}