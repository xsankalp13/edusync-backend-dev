package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrms_payroll_runs")
@Getter
@Setter
@NoArgsConstructor
public class PayrollRun extends AuditableEntity {

    @Column(name = "pay_year", nullable = false)
    private Integer payYear;

    @Column(name = "pay_month", nullable = false)
    private Integer payMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayrollRunStatus status;

    @Column(name = "processed_on", nullable = false)
    private LocalDateTime processedOn;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "total_staff", nullable = false)
    private Integer totalStaff;

    @Column(name = "total_gross", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "total_net", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

