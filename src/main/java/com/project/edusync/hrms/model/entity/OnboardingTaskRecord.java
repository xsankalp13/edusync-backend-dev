package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.TaskRecordStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hrms_onboarding_task_records")
@Getter
@Setter
@NoArgsConstructor
public class OnboardingTaskRecord extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private OnboardingRecord record;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_task_id", nullable = false)
    private OnboardingTemplateTask templateTask;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskRecordStatus status = TaskRecordStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by_ref")
    private UUID completedByRef;

    @Column(name = "completed_by_name", length = 200)
    private String completedByName;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

