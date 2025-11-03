package com.project.edusync.iam.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * The unique permission string.
     * Good practice is to use a "domain:action" format
     * (e.g., "attendance:create", "attendance:read", "users:delete").
     */
    @Column(length = 100, nullable = false, unique = true)
    private String name;

    /**
     * This is the "Soft Delete" flag.
     * We add it here because Users should be de-activated, not deleted.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // --- Relationships ---
    /**
     * The 'mappedBy' indicates that the 'permissions' field in the Role entity
     * is the "owner" of this relationship.
     */
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();
}
