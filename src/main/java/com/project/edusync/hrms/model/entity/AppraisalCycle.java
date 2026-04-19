package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.AppraisalCycleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "hrms_appraisal_cycles")
@Getter
@Setter
@NoArgsConstructor
public class AppraisalCycle extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "cycle_name", nullable = false, length = 200)
    private String cycleName;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppraisalCycleStatus status = AppraisalCycleStatus.DRAFT;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

