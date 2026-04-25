package com.project.edusync.em.model.entity;

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
@Table(name = "exam_entry_decision",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_exam_entry_decision_schedule_student", columnNames = {"exam_schedule_id", "student_id"})
    },
    indexes = {
        @Index(name = "idx_exam_entry_decision_schedule", columnList = "exam_schedule_id")
    })
public class ExamEntryDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_schedule_id", nullable = false)
    private ExamSchedule examSchedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Builder.Default
    @Column(name = "is_allowed", nullable = false)
    private boolean allowed = true;

    @Column(name = "reason", length = 300)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decided_by_staff_id", nullable = false)
    private Staff decidedBy;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        decidedAt = LocalDateTime.now();
    }
}

