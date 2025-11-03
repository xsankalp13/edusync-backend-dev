package com.project.edusync.uis.model.entity.details;

import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.enums.SchoolLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "principal_details")
@Getter
@Setter
@NoArgsConstructor
public class PrincipalDetails {

    /**
     * Shared Primary Key, same as the Staff 'id'.
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

    /**
     * Stores administrative certifications as a JSON string.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "administrative_certifications", columnDefinition = "jsonb")
    private String administrativeCertifications;

    /**
     * This uses the SchoolLevel enum and stores it as a String
     * in the database (e.g., "PRIMARY", "HIGH").
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "school_level_managed", length = 20)
    private SchoolLevel schoolLevelManaged;

}