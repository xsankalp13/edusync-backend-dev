package com.project.edusync.em.model.entity;
import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.em.model.enums.StudentAttendanceStatus;
import com.project.edusync.uis.model.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Entity for examination.student_marks table.
 * Stores the mark for one student in one exam schedule.
 */
@Entity
@Table(name = "student_marks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"schedule_id", "student_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "mark_id"))
public class StudentMark extends AuditableEntity {

    // --- Foreign Keys ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student; // External key to Users.students

    // --- Columns ---

    @Column(name = "marks_obtained", precision = 5, scale = 2)
    private BigDecimal marksObtained;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false)
    private StudentAttendanceStatus attendanceStatus = StudentAttendanceStatus.PRESENT;

    @Column(name = "grade", length = 5)
    private String grade;

    @Lob // Maps to a TEXT column
    @Column(name = "remarks")
    private String remarks;

    // --- Relationships ---

    /**
     * Many marks belong to one ExamSchedule.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ExamSchedule examSchedule;
}