package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.enums.StaffCategory;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "hrms_leave_type_configs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hrms_leave_type_code", columnNames = {"leave_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LeaveTypeConfig extends AuditableEntity {

    @Column(name = "leave_code", nullable = false, length = 20)
    private String leaveCode;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "annual_quota", nullable = false)
    private Integer annualQuota;

    @Column(name = "carry_forward_allowed", nullable = false)
    private boolean carryForwardAllowed = false;

    @Column(name = "max_carry_forward", nullable = false)
    private Integer maxCarryForward = 0;

    @Column(name = "encashment_allowed", nullable = false)
    private boolean encashmentAllowed = false;

    @Column(name = "min_days_before_apply", nullable = false)
    private Integer minDaysBeforeApply = 0;

    @Column(name = "max_consecutive_days")
    private Integer maxConsecutiveDays;

    @Column(name = "requires_document", nullable = false)
    private boolean requiresDocument = false;

    @Column(name = "document_required_after_days")
    private Integer documentRequiredAfterDays;

    @Column(name = "is_paid", nullable = false)
    private boolean isPaid = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "hrms_leave_type_config_grades", joinColumns = @JoinColumn(name = "leave_type_id"))
    @Column(name = "grade_code", length = 30)
    private Set<String> applicableGrades = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "hrms_leave_type_applicable_categories", joinColumns = @JoinColumn(name = "leave_type_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 40)
    private Set<StaffCategory> applicableCategories = new HashSet<>();

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

