package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.FnFStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hrms_fnf_settlements")
@Getter
@Setter
@NoArgsConstructor
public class FullFinalSettlement extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exit_request_id", nullable = false, unique = true)
    private ExitRequest exitRequest;

    @Column(name = "gross_salary_due", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossSalaryDue = BigDecimal.ZERO;

    @Column(name = "deductions", nullable = false, precision = 14, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "leave_encashment", nullable = false, precision = 14, scale = 2)
    private BigDecimal leaveEncashment = BigDecimal.ZERO;

    @Column(name = "gratuity", nullable = false, precision = 14, scale = 2)
    private BigDecimal gratuity = BigDecimal.ZERO;

    @Column(name = "other_additions", nullable = false, precision = 14, scale = 2)
    private BigDecimal otherAdditions = BigDecimal.ZERO;

    @Column(name = "other_deductions", nullable = false, precision = 14, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "net_payable", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayable = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FnFStatus status = FnFStatus.DRAFT;

    @Column(name = "approved_by_ref")
    private UUID approvedByRef;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "remarks", length = 2000)
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

