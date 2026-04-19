package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.ClearanceItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hrms_exit_clearance_items")
@Getter
@Setter
@NoArgsConstructor
public class ExitClearanceItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exit_request_id", nullable = false)
    private ExitRequest exitRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 40)
    private ClearanceItemType itemType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "responsible_party_ref")
    private UUID responsiblePartyRef;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by_name", length = 200)
    private String completedByName;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "waived", nullable = false)
    private boolean waived = false;

    @Column(name = "waived_by", length = 200)
    private String waivedBy;

    @Column(name = "waived_at")
    private LocalDateTime waivedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

