package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "hrms_payroll_entries")
@Getter
@Setter
@NoArgsConstructor
public class PayrollEntry extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mapping_id", nullable = false)
    private StaffSalaryMapping mapping;

    @Column(name = "gross_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal grossPay;

    @Column(name = "total_deductions", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalDeductions;

    @Column(name = "net_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal netPay;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

