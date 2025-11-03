package com.project.edusync.uis.model.entity.details;

import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "librarian_details")
@Getter
@Setter
@NoArgsConstructor
public class LibrarianDetails {

    /**
     * Shared Primary Key: This 'id' is NOT auto-generated.
     * It will be the same Long 'id' as the Staff record it relates to.
     */
    @Id
    @Column(name = "staff_id")
    private Long id;

    /**
     * @MapsId links this @Id to the @OneToOne relationship,
     * telling JPA it's a foreign key.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "staff_id")
    private Staff staff;

    /**
     * Stores specific permissions as a JSON string in a JSONB column.
     * e.g., "[\"manage_checkouts\", \"issue_fines\"]"
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "library_system_permissions", columnDefinition = "jsonb")
    private String librarySystemPermissions;

    /**
     * A boolean flag as specified in your documentation.
     */
    @Column(name = "mlis_degree")
    private boolean mlisDegree;
}