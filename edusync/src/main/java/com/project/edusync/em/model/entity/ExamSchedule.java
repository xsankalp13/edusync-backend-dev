package com.project.edusync.em.model.entity;

import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.model.entity.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity for examination.exam_schedules table.
 * This is the specific schedule for one subject in an exam.
 */
@Entity
@Table(name = "exam_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private AcademicClass academicClass; // External key to Academics.classes

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id") // This relationship is optional
    private Section section; // External key to Academics.sections

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject; // External key to Academics.subjects

    // --- Columns ---

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "passing_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal passingMarks;

    @Column(name = "room_number", length = 50)
    private String roomNumber;

    // --- Relationships ---

    /**
     * Many schedules belong to one Exam.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    /**
     * An ExamSchedule has many StudentMarks (one for each student).
     */
    @OneToMany(
            mappedBy = "examSchedule",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private Set<StudentMark> studentMarks = new HashSet<>();

    /**
     * An ExamSchedule has one QuestionPaper.
     */
    @OneToOne(
            mappedBy = "examSchedule",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private QuestionPaper questionPaper;
}