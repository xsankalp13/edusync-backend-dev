package com.project.edusync.uis.model.entity;
import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "guardians")
@Getter
@Setter
@NoArgsConstructor
public class Guardian extends AuditableEntity {

    // id, uuid, and audit fields are inherited.
    // All name/dob columns are REMOVED.

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    // Email is also removed, as it's part of the UserProfile
    // or the User (login) record.

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // --- Relationships ---

    /**
     * This links the Guardian "Role" to the "Person" (UserProfile).
     * The 'profile_id' column must be unique.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", referencedColumnName = "id", unique = true)
    private UserProfile userProfile;

    /**
     * Relationship to students. This remains the same.
     */
    @OneToMany(mappedBy = "guardian", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentGuardianRelationship> studentRelationships = new HashSet<>();
}