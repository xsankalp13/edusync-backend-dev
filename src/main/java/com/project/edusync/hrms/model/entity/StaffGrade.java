package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.TeachingWing;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "hrms_staff_grades",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hrms_staff_grade_code", columnNames = {"grade_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StaffGrade extends AuditableEntity {

    @Column(name = "grade_code", nullable = false, length = 30)
    private String gradeCode;

    @Column(name = "grade_name", nullable = false, length = 120)
    private String gradeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "teaching_wing", nullable = false, length = 30)
    private TeachingWing teachingWing;

    @Column(name = "pay_band_min", nullable = false, precision = 12, scale = 2)
    private BigDecimal payBandMin;

    @Column(name = "pay_band_max", nullable = false, precision = 12, scale = 2)
    private BigDecimal payBandMax;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "min_years_for_promotion")
    private Integer minYearsForPromotion;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

