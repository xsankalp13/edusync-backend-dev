package com.project.edusync.uis.model.entity.details;

import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.enums.Ethnicity;
import com.project.edusync.uis.model.enums.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "student_demographics")
@Getter
@Setter
@NoArgsConstructor
public class StudentDemographics {

    /**
     * Shared Primary Key: This 'id' is NOT auto-generated.
     * It will be the same Long 'id' as the Student record.
     */
    @Id
    @Column(name = "student_id")
    private Long id;

    /**
     * @MapsId links this @Id to the @OneToOne relationship.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "student_id")
    private Student student;

    // --- Demographic Fields ---

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Ethnicity ethnicity;

    @Column(length = 100)
    private String nationality;

    @Column(name = "language_primary", length = 50)
    private String languagePrimary;

    @Column(name = "language_secondary", length = 50)
    private String languageSecondary;

    /**
     * A JSONB column for semi-structured data about home environment,
     * as specified in your documentation.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "home_environment_details", columnDefinition = "jsonb")
    private String homeEnvironmentDetails; // e.g., "{\"siblings\": 2, \"primary_guardian\": \"Mother\"}"
}
