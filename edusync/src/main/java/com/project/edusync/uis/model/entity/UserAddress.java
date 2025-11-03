package com.project.edusync.uis.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.enums.AddressType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_addresses",
        uniqueConstraints = {
                // A user can only have one of each address type
                // (e.g., only one "HOME" address).
                @UniqueConstraint(columnNames = {"profile_id", "address_type"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class UserAddress extends AuditableEntity {

    // id, uuid, and audit fields are inherited.

    // --- Relationships ---

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id")
    private Address address;

    // --- Payload Fields ---

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", length = 20, nullable = false)
    private AddressType addressType;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;
}