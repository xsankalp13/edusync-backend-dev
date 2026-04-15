package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hrms_approval_step_records")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalStepRecord extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ApprovalRequest request;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "approver_role", nullable = false, length = 100)
    private String approverRole;

    @Column(name = "approver_ref")
    private UUID approverRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "acted_at")
    private LocalDateTime actedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

