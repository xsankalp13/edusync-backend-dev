package com.project.edusync.uis.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.enums.StudentLeaveStatus;
import com.project.edusync.uis.model.enums.StudentLeaveType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "student_leave_applications")
@Getter
@Setter
@NoArgsConstructor
public class StudentLeaveApplication extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by_guardian_id")
    private Guardian appliedBy;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", length = 20)
    private StudentLeaveType leaveType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private StudentLeaveStatus status;

    @Column(name = "half_day", nullable = false)
    private boolean halfDay = false;

}

