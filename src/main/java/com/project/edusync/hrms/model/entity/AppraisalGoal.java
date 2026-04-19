package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hrms_appraisal_goals")
@Getter
@Setter
@NoArgsConstructor
public class AppraisalGoal extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AppraisalCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "goal_title", nullable = false, length = 300)
    private String goalTitle;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "weightage", nullable = false)
    private int weightage = 0;

    @Column(name = "target_metric", length = 500)
    private String targetMetric;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

