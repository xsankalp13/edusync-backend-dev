package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.HalfDayType;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrms_leave_applications")
@Getter
@Setter
@NoArgsConstructor
public class LeaveApplication extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveTypeConfig leaveType;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "total_days", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDays;

    @Column(name = "is_half_day", nullable = false)
    private boolean halfDay = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_type", length = 20)
    private HalfDayType halfDayType;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "attachment_url", length = 1024)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveApplicationStatus status;

    @Column(name = "applied_on", nullable = false)
    private LocalDateTime appliedOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_staff_id")
    private Staff reviewedBy;

    @Column(name = "reviewed_on")
    private LocalDateTime reviewedOn;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_by_name", length = 200)
    private String reviewedByName;

    @Column(name = "review_remarks", length = 500)
    private String reviewRemarks;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

