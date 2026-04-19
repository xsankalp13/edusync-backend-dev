package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.PromotionStatus;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrms_promotion_requests")
@Getter
@Setter
@NoArgsConstructor
public class PromotionRequest extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_designation_id", nullable = false)
    private StaffDesignation currentDesignation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_designation_id", nullable = false)
    private StaffDesignation proposedDesignation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_grade_id")
    private StaffGrade currentGrade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_grade_id")
    private StaffGrade proposedGrade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_salary_template_id")
    private SalaryTemplate newSalaryTemplate;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PromotionStatus status;

    @Column(name = "initiated_by_user_id", nullable = false)
    private Long initiatedByUserId;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_by_name", length = 150)
    private String approvedByName;

    @Column(name = "approved_on")
    private LocalDateTime approvedOn;

    @Column(name = "order_reference", length = 100)
    private String orderReference;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
