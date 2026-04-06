package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.DayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "hrms_calendar_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hrms_calendar_year_date", columnNames = {"academic_year", "event_date"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AcademicCalendarEvent extends AuditableEntity {

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "event_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 32)
    private DayType dayType;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "applies_to_staff", nullable = false)
    private boolean appliesToStaff = true;

    @Column(name = "applies_to_students", nullable = false)
    private boolean appliesToStudents = true;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}

