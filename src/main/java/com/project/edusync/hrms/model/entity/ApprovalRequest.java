package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.ApprovalActionType;
import com.project.edusync.hrms.model.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hrms_approval_requests")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalRequest extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ApprovalActionType actionType;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_ref")
    private UUID entityRef;

    @Column(name = "requested_by_ref")
    private UUID requestedByRef;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "current_step_order", nullable = false)
    private int currentStepOrder = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_status", nullable = false, length = 20)
    private ApprovalStatus finalStatus = ApprovalStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

