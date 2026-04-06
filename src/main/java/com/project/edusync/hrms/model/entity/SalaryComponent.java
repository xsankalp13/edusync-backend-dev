package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.SalaryCalculationMethod;
import com.project.edusync.hrms.model.enums.SalaryComponentType;
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
        name = "hrms_salary_components",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hrms_salary_component_code", columnNames = {"component_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SalaryComponent extends AuditableEntity {

    @Column(name = "component_code", nullable = false, length = 40)
    private String componentCode;

    @Column(name = "component_name", nullable = false, length = 120)
    private String componentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 20)
    private SalaryComponentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 40)
    private SalaryCalculationMethod calculationMethod;

    @Column(name = "default_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal defaultValue;

    @Column(name = "is_taxable", nullable = false)
    private boolean taxable = true;

    @Column(name = "is_statutory", nullable = false)
    private boolean statutory = false;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

