package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "hrms_salary_template_components",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hrms_template_component", columnNames = {"template_id", "component_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SalaryTemplateComponent extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private SalaryTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private SalaryComponent component;

    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

