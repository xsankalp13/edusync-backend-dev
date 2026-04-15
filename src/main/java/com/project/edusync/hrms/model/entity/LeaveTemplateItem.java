package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hrms_leave_template_items")
@Getter
@Setter
@NoArgsConstructor
public class LeaveTemplateItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private LeaveTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveTypeConfig leaveType;

    @Column(name = "custom_quota")
    private Integer customQuota;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
