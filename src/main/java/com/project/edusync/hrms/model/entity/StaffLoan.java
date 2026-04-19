package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.LoanStatus;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hrms_staff_loans")
@Getter
@Setter
@NoArgsConstructor
public class StaffLoan extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "loan_type", nullable = false, length = 100)
    private String loanType;

    @Column(name = "principal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "approved_amount", precision = 14, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "disbursed_at")
    private LocalDate disbursedAt;

    @Column(name = "emi_amount", precision = 14, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "emi_count")
    private Integer emiCount;

    @Column(name = "remaining_emis")
    private Integer remainingEmis;

    @Column(name = "interest_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "approval_request_ref")
    private UUID approvalRequestRef;

    @Column(name = "remarks", length = 2000)
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

