package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.OvertimeStatus;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hrms_overtime_records")
@Getter
@Setter
@NoArgsConstructor
public class OvertimeRecord extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "hours_worked", nullable = false, precision = 5, scale = 2)
    private BigDecimal hoursWorked;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OvertimeStatus status = OvertimeStatus.PENDING;

    @Column(name = "approved_by_ref")
    private UUID approvedByRef;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "compensation_type", nullable = false, length = 20)
    private String compensationType = "CASH";

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_remarks", length = 500)
    private String rejectionRemarks;

    @Column(name = "payroll_run_ref")
    private UUID payrollRunRef;

    @Column(name = "multiplier", precision = 4, scale = 2)
    private BigDecimal multiplier = new BigDecimal("1.50");

    @Column(name = "approved_amount", precision = 10, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

