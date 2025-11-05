package com.project.edusync.em.model.entity;


import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.em.model.enums.ExamType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.FileStore;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity for examination.exams table.
 * This is the high-level exam event (e.g., "Midterm 2025").
 */
@Entity
@Table(name = "exams", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "academic_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "exam_id"))
public class Exam extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "academic_year", nullable = false, length = 10)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    private ExamType examType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    // --- Relationships ---

    /**
     * An Exam has many ExamSchedules (one for each subject).
     */
    @OneToMany(
            mappedBy = "exam",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private Set<ExamSchedule> schedules = new HashSet<>();

}

