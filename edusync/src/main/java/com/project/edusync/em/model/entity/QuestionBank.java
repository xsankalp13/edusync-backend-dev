package com.project.edusync.em.model.entity;

import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Subject;
import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.em.model.enums.DifficultyLevel;
import com.project.edusync.em.model.enums.QuestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity for examination.question_bank table.
 * A single, reusable question.
 */
@Entity
@Table(name = "question_bank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "question_id")),
        @AttributeOverride(name = "createdBy", column = @Column(name = "created_by")),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by"))
})
public class QuestionBank extends AuditableEntity {

    // --- Foreign Keys ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject; // External key to Academics.subjects

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private AcademicClass academicClass; // External key to Academics.classes

    // --- Columns ---

    @Column(name = "topic", length = 255)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false)
    private DifficultyLevel difficultyLevel = DifficultyLevel.MEDIUM;

    @Lob
    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Lob
    @Column(name = "option_a")
    private String optionA;

    @Lob
    @Column(name = "option_b")
    private String optionB;

    @Lob
    @Column(name = "option_c")
    private String optionC;

    @Lob
    @Column(name = "option_d")
    private String optionD;

    @Lob
    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "marks", nullable = false, precision = 4, scale = 2)
    private BigDecimal marks;

    // --- Relationships ---

    /**
     * A Question can be used in many different papers.
     */
    @OneToMany(
            mappedBy = "question",
            cascade = CascadeType.ALL, // If a question is deleted, remove its mappings
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private Set<PaperQuestionMap> paperMappings = new HashSet<>();
}