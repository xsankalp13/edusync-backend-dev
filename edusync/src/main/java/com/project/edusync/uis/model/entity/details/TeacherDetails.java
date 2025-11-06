package com.project.edusync.uis.model.entity.details;

import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.enums.EducationLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "teacher_details")
@Getter
@Setter
@NoArgsConstructor
public class TeacherDetails {

    /**
     * This is the Primary Key. It is NOT auto-generated.
     * It will be the same 'id' value as the Staff member it belongs to.
     */
    @Id
    @Column(name = "staff_id")
    private Long id;

    /**
     * This @MapsId annotation tells JPA:
     * "My '@Id' field (above) gets its value from this @OneToOne relationship."
     * This perfectly creates the shared primary key constraint.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "staff_id")
    private Staff staff;

    // --- Teacher-Specific Fields ---

    @Column(name = "state_license_number", length = 50)
    private String stateLicenseNumber;

    /**
     * Store as the Enum, mapped to a string in the DB.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", length = 50)
    private EducationLevel educationLevel; // e.g., "Bachelors", "Masters"

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    /**
     * Using @JdbcTypeCode to map to a PostgreSQL JSONB column.
     * This is the best way to store lists of unstructured data.
     * We store it as a String, but the DB treats it as queryable JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> specializations; // e.g., "[\"Physics\", \"Calculus\"]"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> certifications;// e.g., "[\"First Aid\", \"Google Certified\"]"

}
