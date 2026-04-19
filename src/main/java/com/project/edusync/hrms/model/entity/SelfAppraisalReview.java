package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hrms_self_appraisal_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cycle_id", "staff_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SelfAppraisalReview extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AppraisalCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "self_rating")
    private Integer selfRating;

    @Column(name = "achievements", length = 3000)
    private String achievements;

    @Column(name = "challenges", length = 3000)
    private String challenges;

    @Column(name = "training_needs", length = 2000)
    private String trainingNeeds;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

