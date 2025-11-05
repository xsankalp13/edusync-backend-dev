package com.project.edusync.em.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Entity for examination.grade_scales table.
 * A single rule within a grading system (e.g., A1 = 91-100).
 */
@Entity
@Table(name = "grade_scales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradeScale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_scale_id")
    private Long gradeScaleId;

    @Column(name = "grade_name", nullable = false, length = 10)
    private String gradeName;

    @Column(name = "min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPercentage;

    @Column(name = "max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercentage;

    @Column(name = "grade_points", precision = 3, scale = 1)
    private BigDecimal gradePoints;

    // --- Relationships ---

    /**
     * Many scales belong to one GradeSystem.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_system_id", nullable = false)
    private GradeSystem gradeSystem;
}

