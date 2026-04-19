package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.em.model.entity.QuestionMark;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshot;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshotQuestion;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshotSection;
import com.project.edusync.em.model.enums.TemplateQuestionType;
import com.project.edusync.em.model.enums.TemplateSectionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvaluationScoringCalculatorTest {

    @Test
    void computesMaxForInternalChoiceWithoutSummingOptions() {
        TemplateSnapshot snapshot = TemplateSnapshot.builder()
                .sections(List.of(
                        TemplateSnapshotSection.builder()
                                .name("A")
                                .sectionOrder(1)
                                .sectionType(TemplateSectionType.FIXED)
                                .totalQuestions(1)
                                .attemptQuestions(1)
                                .questions(List.of(
                                        TemplateSnapshotQuestion.builder()
                                                .questionNo(1)
                                                .marks(10)
                                                .type(TemplateQuestionType.INTERNAL_CHOICE)
                                                .options(List.of("a", "b"))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        List<QuestionMark> marks = List.of(
                QuestionMark.builder().sectionName("A").questionNumber(1).optionLabel("a").marksObtained(new BigDecimal("4")).build(),
                QuestionMark.builder().sectionName("A").questionNumber(1).optionLabel("b").marksObtained(new BigDecimal("7")).build()
        );

        EvaluationScoringCalculator.ScoreComputationResult result = EvaluationScoringCalculator.compute(snapshot, marks);

        assertEquals(new BigDecimal("7"), result.totalMarks());
        assertEquals(new BigDecimal("7"), result.sectionTotals().get("A"));
        assertEquals(List.of("1(b)"), result.selectedQuestions().get("A"));
    }

    @Test
    void computesBestNForOptionalSections() {
        TemplateSnapshot snapshot = TemplateSnapshot.builder()
                .sections(List.of(
                        TemplateSnapshotSection.builder()
                                .name("B")
                                .sectionOrder(1)
                                .sectionType(TemplateSectionType.OPTIONAL)
                                .totalQuestions(3)
                                .attemptQuestions(2)
                                .questions(List.of(
                                        TemplateSnapshotQuestion.builder().questionNo(1).marks(10).type(TemplateQuestionType.NORMAL).build(),
                                        TemplateSnapshotQuestion.builder().questionNo(2).marks(10).type(TemplateQuestionType.NORMAL).build(),
                                        TemplateSnapshotQuestion.builder().questionNo(3).marks(10).type(TemplateQuestionType.NORMAL).build()
                                ))
                                .build()
                ))
                .build();

        List<QuestionMark> marks = List.of(
                QuestionMark.builder().sectionName("B").questionNumber(1).optionLabel("").marksObtained(new BigDecimal("2")).build(),
                QuestionMark.builder().sectionName("B").questionNumber(2).optionLabel("").marksObtained(new BigDecimal("9")).build(),
                QuestionMark.builder().sectionName("B").questionNumber(3).optionLabel("").marksObtained(new BigDecimal("5")).build()
        );

        EvaluationScoringCalculator.ScoreComputationResult result = EvaluationScoringCalculator.compute(snapshot, marks);

        assertEquals(new BigDecimal("14"), result.totalMarks());
        assertEquals(List.of("2", "3"), result.selectedQuestions().get("B"));
    }

    @Test
    void handlesFewerAnswersThanAttemptCountByUsingAvailableMarks() {
        TemplateSnapshot snapshot = TemplateSnapshot.builder()
                .sections(List.of(
                        TemplateSnapshotSection.builder()
                                .name("C")
                                .sectionOrder(1)
                                .sectionType(TemplateSectionType.OPTIONAL)
                                .totalQuestions(3)
                                .attemptQuestions(2)
                                .marksPerQuestion(10)
                                .build()
                ))
                .build();

        List<QuestionMark> marks = List.of(
                QuestionMark.builder().sectionName("C").questionNumber(1).optionLabel("").marksObtained(new BigDecimal("8")).build()
        );

        EvaluationScoringCalculator.ScoreComputationResult result = EvaluationScoringCalculator.compute(snapshot, marks);

        assertEquals(new BigDecimal("8"), result.totalMarks());
        assertEquals(new BigDecimal("8"), result.sectionTotals().get("C"));
    }
}

