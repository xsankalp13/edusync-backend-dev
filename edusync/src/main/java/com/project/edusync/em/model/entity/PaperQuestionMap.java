package com.project.edusync.em.model.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Entity for examination.paper_questions_map table.
 * This is the "join table" that links a QuestionPaper to a QuestionBank.
 */
@Entity
@Table(name = "paper_questions_map", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"paper_id", "question_id"}),
        @UniqueConstraint(columnNames = {"paper_id", "question_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaperQuestionMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paper_question_id")
    private Long paperQuestionId;

    @Column(name = "question_number", nullable = false, length = 10)
    private String questionNumber;

    @Column(name = "marks_for_question", nullable = false, precision = 4, scale = 2)
    private BigDecimal marksForQuestion;

    // --- Relationships ---

    /**
     * Many mappings belong to one QuestionPaper.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private QuestionPaper questionPaper;

    /**
     * Many mappings link to one Question from the bank.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionBank question;
}
