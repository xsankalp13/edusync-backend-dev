package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.entity.Staff;
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
        name = "hrms_leave_balances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hrms_leave_balance_staff_type_year",
                        columnNames = {"staff_id", "leave_type_id", "academic_year"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LeaveBalance extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveTypeConfig leaveType;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "total_quota", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalQuota = BigDecimal.ZERO;

    @Column(name = "used", nullable = false, precision = 10, scale = 2)
    private BigDecimal used = BigDecimal.ZERO;

    @Column(name = "carried_forward", nullable = false, precision = 10, scale = 2)
    private BigDecimal carriedForward = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public BigDecimal getRemaining() {
        return totalQuota.subtract(used);
    }
}

