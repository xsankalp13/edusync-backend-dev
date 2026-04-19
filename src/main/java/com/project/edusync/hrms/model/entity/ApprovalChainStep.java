package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hrms_approval_chain_steps")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalChainStep extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chain_config_id", nullable = false)
    private ApprovalChainConfig chainConfig;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "approver_role", nullable = false, length = 100)
    private String approverRole;

    @Column(name = "step_label", length = 200)
    private String stepLabel;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

