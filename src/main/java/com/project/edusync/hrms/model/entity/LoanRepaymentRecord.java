package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.RepaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hrms_loan_repayments")
@Getter
@Setter
@NoArgsConstructor
public class LoanRepaymentRecord extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private StaffLoan loan;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payroll_run_ref")
    private UUID payrollRunRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RepaymentStatus status = RepaymentStatus.SCHEDULED;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

