package com.project.edusync.em.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.em.model.enums.TemplateQuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "template_question", uniqueConstraints = {
        @UniqueConstraint(name = "uq_template_question_section_no", columnNames = {"section_id", "question_no"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "id", column = @Column(name = "question_id"))
public class TemplateQuestion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private TemplateSection section;

    @Column(name = "question_no", nullable = false)
    private Integer questionNo;

    @Column(name = "marks", nullable = false)
    private Integer marks;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    @Builder.Default
    private TemplateQuestionType type = TemplateQuestionType.NORMAL;

    @ElementCollection
    @CollectionTable(name = "question_option", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "label", nullable = false, length = 10)
    @Builder.Default
    private List<String> options = new ArrayList<>();
}

