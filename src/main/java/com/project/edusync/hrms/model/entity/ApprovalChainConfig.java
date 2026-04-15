package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.ApprovalActionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hrms_approval_chain_configs")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalChainConfig extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ApprovalActionType actionType;

    @Column(name = "chain_name", nullable = false, length = 200)
    private String chainName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

