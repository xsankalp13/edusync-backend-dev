package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.AssignedParty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hrms_onboarding_template_tasks")
@Getter
@Setter
@NoArgsConstructor
public class OnboardingTemplateTask extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private OnboardingTemplate template;

    @Column(name = "task_order", nullable = false)
    private int taskOrder = 0;

    @Column(name = "task_title", nullable = false, length = 300)
    private String taskTitle;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "due_after_days", nullable = false)
    private int dueAfterDays = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_party", nullable = false, length = 20)
    private AssignedParty assignedParty = AssignedParty.HR;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

