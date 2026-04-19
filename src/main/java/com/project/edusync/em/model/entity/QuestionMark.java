package com.project.edusync.em.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.em.model.enums.AnnotationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "question_marks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"evaluation_result_id", "section_name", "question_number", "option_label"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "id", column = @Column(name = "question_mark_id"))
public class QuestionMark extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_result_id", nullable = false)
    private EvaluationResult evaluationResult;

    @Column(name = "section_name", nullable = false, length = 100)
    private String sectionName;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "option_label", nullable = false, length = 10)
    private String optionLabel;

    @Column(name = "marks_obtained", nullable = false, precision = 7, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "max_marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal maxMarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "annotation_type", nullable = false, length = 10)
    private AnnotationType annotationType = AnnotationType.NONE;
}

