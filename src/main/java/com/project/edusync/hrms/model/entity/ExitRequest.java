package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.ExitRequestStatus;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hrms_exit_requests")
@Getter
@Setter
@NoArgsConstructor
public class ExitRequest extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "resignation_date", nullable = false)
    private LocalDate resignationDate;

    @Column(name = "last_working_date")
    private LocalDate lastWorkingDate;

    @Column(name = "exit_reason", length = 2000)
    private String exitReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExitRequestStatus status = ExitRequestStatus.SUBMITTED;

    @Column(name = "initiated_by_ref")
    private UUID initiatedByRef;

    @Column(name = "approval_request_ref")
    private UUID approvalRequestRef;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

