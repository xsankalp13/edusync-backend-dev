package com.project.edusync.em.model.entity;

import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.em.model.enums.ExamAttendanceStatus;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "exam_attendance",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_exam_attendance_schedule_student", columnNames = {"exam_schedule_id", "student_id"})
    },
    indexes = {
        @Index(name = "idx_exam_attendance_schedule_room", columnList = "exam_schedule_id, room_id"),
        @Index(name = "idx_exam_attendance_marked_by", columnList = "marked_by_staff_id")
    })
public class ExamAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_schedule_id", nullable = false)
    private ExamSchedule examSchedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExamAttendanceStatus status;

    @Builder.Default
    @Column(name = "malpractice_reported", nullable = false)
    private boolean malpracticeReported = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_staff_id")
    private Staff markedBy;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime timestamp;

    @Builder.Default
    @Column(nullable = false)
    private boolean finalized = false;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

