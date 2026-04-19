package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrms_compoff_records")
@Getter
@Setter
@NoArgsConstructor
public class CompOffRecord extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "overtime_record_id")
    private OvertimeRecord overtimeRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id")
    private LeaveTypeConfig leaveType;

    @Column(name = "credit_date", nullable = false)
    private LocalDate creditDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "credited", nullable = false)
    private boolean credited = false;

    @Column(name = "credited_at")
    private LocalDateTime creditedAt;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

