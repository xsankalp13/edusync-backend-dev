package com.project.edusync.ams.model.entity;

import com.project.edusync.ams.model.enums.LateClockInStatus;
import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Captures a late / out-of-window clock-in event that requires admin review.
 * Created automatically by AttendanceCronJobs or by the attendance service when
 * a staff member clocks in after the configured maxLateThreshold.
 */
@Entity
@Table(name = "late_clockin_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "attendance_date"}))
@Getter
@Setter
@NoArgsConstructor
public class LateClockInRequest extends AuditableEntity {

    /** Logical FK to uis.Staff.id */
    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** Actual clock-in time recorded by the system */
    @Column(name = "clock_in_time")
    private LocalTime clockInTime;

    /** Minutes late beyond the shift start + grace period */
    @Column(name = "minutes_late", nullable = false)
    private Integer minutesLate;

    /** Staff's own justification / reason */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LateClockInStatus status = LateClockInStatus.PENDING;

    /** Admin reviewer's remarks on approval/rejection */
    @Column(name = "admin_remarks", columnDefinition = "TEXT")
    private String adminRemarks;

    /** User ID of the admin who reviewed */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    /** Link to the attendance record this request is associated with */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private StaffDailyAttendance attendance;
}
