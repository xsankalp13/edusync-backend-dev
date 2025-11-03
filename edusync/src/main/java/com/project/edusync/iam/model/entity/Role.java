package com.project.edusync.iam.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.Permissions;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role extends AuditableEntity {

    /**
     * The unique name of the role (e.g., "TEACHER", "ADMIN").
     */
    @Column(length = 50, nullable = false, unique = true)
    private String name;

    /**
     * This is the "Soft Delete" flag.
     * We add it here because Users should be de-activated, not deleted.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // --- Relationships ---
    /**
     * Defines the permissions that this role grants.
     * This is a ManyToMany relationship, mapping to the 'role_permissions' junction table.
     * Like with users, FetchType.LAZY is essential for performance.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permissions> permissions = new HashSet<>();

    /**
     * 'mappedBy' indicates that the 'roles' field in the User entity
     * is the "owner" of this relationship. This is the inverse side
     * and is not strictly necessary, but can be useful for navigation.
     */
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();
}
