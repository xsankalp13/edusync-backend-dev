package com.project.edusync.uis.model.entity.details;

import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "security_guard_details")
@Getter
@Setter
@NoArgsConstructor
public class SecurityGuardDetails {

    /**
     * Shared Primary Key: This 'id' is NOT auto-generated.
     * It will be the same Long 'id' as the Staff record it relates to.
     */
    @Id
    @Column(name = "staff_id")
    private Long id;

    /**
     * @MapsId links this @Id to the @OneToOne relationship.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "staff_id")
    private Staff staff;

    // --- Security-Specific Fields ---

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_expiry_date")
    private java.time.LocalDate licenseExpiryDate;

    /**
     * Stores assigned patrol zones as a JSON string.
     * e.g., "[\"Zone A\", \"Main Gate\"]"
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assigned_zones", columnDefinition = "jsonb")
    private String assignedZones;
}