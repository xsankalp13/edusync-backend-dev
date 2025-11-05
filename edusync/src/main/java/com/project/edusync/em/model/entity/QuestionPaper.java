package com.project.edusync.em.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity for examination.question_papers table.
 * The "header" or "cover page" for a generated paper.
 */
@Entity
@Table(name = "question_papers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "paper_id")),
        @AttributeOverride(name = "createdBy", column = @Column(name = "generated_by")), // Remapping createdBy
        @AttributeOverride(name = "createdAt", column = @Column(name = "generated_at"))  // Remapping createdAt
})
public class QuestionPaper extends AuditableEntity {

    // --- Columns ---

    @Column(name = "paper_name", nullable = false, length = 255)
    private String paperName;

    @Column(name = "total_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Lob
    @Column(name = "instructions")
    private String instructions;

    // --- Relationships ---

    /**
     * A QuestionPaper is generated for one specific ExamSchedule.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false, unique = true)
    private ExamSchedule examSchedule;

    /**
     * A QuestionPaper has many question mappings (the actual questions).
     */
    @OneToMany(
            mappedBy = "questionPaper",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private Set<PaperQuestionMap> questionMappings = new HashSet<>();
}

