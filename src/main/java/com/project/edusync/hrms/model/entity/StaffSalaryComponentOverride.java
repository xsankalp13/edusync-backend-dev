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
        name = "hrms_staff_salary_component_overrides",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hrms_mapping_component_override", columnNames = {"mapping_id", "component_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StaffSalaryComponentOverride extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mapping_id", nullable = false)
    private StaffSalaryMapping mapping;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private SalaryComponent component;

    @Column(name = "override_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal overrideValue;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

