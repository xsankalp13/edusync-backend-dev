package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "hrms_training_courses")
@Getter
@Setter
@NoArgsConstructor
public class TrainingCourse extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "course_code", nullable = false, length = 50, unique = true)
    private String courseCode;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", length = 3000)
    private String description;

    @Column(name = "facilitator", length = 200)
    private String facilitator;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "max_seats")
    private Integer maxSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CourseStatus status = CourseStatus.UPCOMING;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

