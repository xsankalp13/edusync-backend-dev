package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.enums.StaffCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hrms_salary_templates")
@Getter
@Setter
@NoArgsConstructor
public class SalaryTemplate extends AuditableEntity {

    @Column(name = "template_name", nullable = false, length = 150)
    private String templateName;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id")
    private StaffGrade grade;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_category", length = 40)
    private StaffCategory applicableCategory;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

