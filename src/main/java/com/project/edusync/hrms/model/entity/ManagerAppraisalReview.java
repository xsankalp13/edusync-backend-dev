package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hrms_manager_appraisal_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cycle_id", "staff_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ManagerAppraisalReview extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AppraisalCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_staff_id")
    private Staff reviewerStaff;

    @Column(name = "manager_rating")
    private Integer managerRating;

    @Column(name = "strengths", length = 3000)
    private String strengths;

    @Column(name = "areas_of_improvement", length = 3000)
    private String areasOfImprovement;

    @Column(name = "overall_remarks", length = 2000)
    private String overallRemarks;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

