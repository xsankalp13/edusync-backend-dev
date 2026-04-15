package com.project.edusync.iam.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User extends AuditableEntity implements UserDetails {

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @Column(length = 100, unique = true)
    private String email;

    @Column(length = 20)
    private String mobile;

    @Column(length = 255, nullable = false)
    private String password;

    /**
     * This is the "Soft Delete" flag.
     * We add it here because Users should be de-activated, not deleted.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "last_login_timestamp")
    private LocalDateTime lastLoginTimestamp;

    // --- Relationships ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> authorities = new LinkedHashSet<>();

        // Keep role authorities for backward compatibility.
        roles.stream()
                .map(Role::getName)
                .filter(name -> name != null && !name.isBlank())
                .forEach(authorities::add);

        // Add permission authorities used by fine-grained @PreAuthorize checks.
        roles.stream()
                .filter(role -> role.getPermissions() != null)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .filter(name -> name != null && !name.isBlank())
                .forEach(authorities::add);

        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}