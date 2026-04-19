package com.project.edusync.em.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.em.model.enums.TemplateSectionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "template_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "id", column = @Column(name = "section_id"))
public class TemplateSection extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ExamTemplate template;

    @Column(name = "section_name", nullable = false, length = 80)
    private String sectionName;

    @Column(name = "section_order", nullable = false)
    private Integer sectionOrder;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Column(name = "marks_per_question", nullable = false)
    private Integer marksPerQuestion;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "attempt_questions")
    private Integer attemptQuestions;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", length = 20)
    private TemplateSectionType sectionType;

    @Column(name = "internal_choice_enabled")
    private Boolean internalChoiceEnabled;

    @Column(name = "is_objective")
    private Boolean isObjective;

    @Column(name = "is_subjective")
    private Boolean isSubjective;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<com.project.edusync.em.model.entity.TemplateQuestion> questions = new ArrayList<>();
}

