package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.em.model.entity.QuestionMark;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshot;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshotQuestion;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshotSection;
import com.project.edusync.em.model.enums.TemplateQuestionType;
import com.project.edusync.em.model.enums.TemplateSectionType;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

final class EvaluationScoringCalculator {

    private EvaluationScoringCalculator() {
    }

    static ScoreComputationResult compute(TemplateSnapshot snapshot, List<QuestionMark> marks) {
        Map<String, BigDecimal> sectionTotals = new LinkedHashMap<>();
        Map<String, List<String>> selectedQuestions = new LinkedHashMap<>();

        Map<String, BigDecimal> markByEntryKey = new HashMap<>();
        for (QuestionMark mark : marks) {
            String key = entryKey(mark.getSectionName(), mark.getQuestionNumber(), normalizeOptionLabel(mark.getOptionLabel()));
            markByEntryKey.put(key, mark.getMarksObtained() == null ? BigDecimal.ZERO : mark.getMarksObtained());
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<TemplateSnapshotSection> sections = snapshot.getSections() == null
                ? List.of()
                : snapshot.getSections().stream()
                .sorted(Comparator.comparing(TemplateSnapshotSection::getSectionOrder))
                .collect(Collectors.toList());

        for (TemplateSnapshotSection section : sections) {
            List<TemplateSnapshotQuestion> questions = materializeQuestions(section);
            List<ResolvedQuestionScore> resolvedScores = new ArrayList<>();
            for (TemplateSnapshotQuestion question : questions) {
                resolvedScores.add(resolveQuestionScore(section.getName(), question, markByEntryKey));
            }

            List<ResolvedQuestionScore> selected = selectQuestions(section, resolvedScores);
            BigDecimal sectionTotal = selected.stream()
                    .map(ResolvedQuestionScore::marks)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            sectionTotals.put(section.getName(), sectionTotal);
            selectedQuestions.put(section.getName(), selected.stream().map(ResolvedQuestionScore::auditKey).collect(Collectors.toList()));
            grandTotal = grandTotal.add(sectionTotal);
        }

        return new ScoreComputationResult(grandTotal, sectionTotals, selectedQuestions);
    }

    private static List<ResolvedQuestionScore> selectQuestions(TemplateSnapshotSection section,
                                                               List<ResolvedQuestionScore> scores) {
        if (resolveSectionType(section) != TemplateSectionType.OPTIONAL) {
            return scores;
        }
        int attempt = resolveAttemptQuestions(section);
        return scores.stream()
                .sorted(Comparator.comparing(ResolvedQuestionScore::marks).reversed()
                        .thenComparing(ResolvedQuestionScore::questionNo))
                .limit(Math.max(attempt, 0))
                .collect(Collectors.toList());
    }

    private static ResolvedQuestionScore resolveQuestionScore(String sectionName,
                                                              TemplateSnapshotQuestion question,
                                                              Map<String, BigDecimal> markByEntryKey) {
        TemplateQuestionType type = question.getType() == null ? TemplateQuestionType.NORMAL : question.getType();
        if (type != TemplateQuestionType.INTERNAL_CHOICE) {
            BigDecimal marks = markByEntryKey.getOrDefault(entryKey(sectionName, question.getQuestionNo(), null), BigDecimal.ZERO);
            return new ResolvedQuestionScore(question.getQuestionNo(), marks, String.valueOf(question.getQuestionNo()));
        }

        BigDecimal max = BigDecimal.ZERO;
        String selectedOption = null;
        List<String> options = question.getOptions() == null ? List.of() : question.getOptions();
        for (String option : options) {
            BigDecimal value = markByEntryKey.getOrDefault(entryKey(sectionName, question.getQuestionNo(), normalizeOptionLabel(option)), BigDecimal.ZERO);
            if (value.compareTo(max) >= 0) {
                max = value;
                selectedOption = option;
            }
        }

        // Backward compatibility: handle direct/internal marks saved without option labels.
        BigDecimal direct = markByEntryKey.getOrDefault(entryKey(sectionName, question.getQuestionNo(), null), BigDecimal.ZERO);
        if (direct.compareTo(max) > 0) {
            max = direct;
            selectedOption = null;
        }

        String auditKey = selectedOption == null
                ? String.valueOf(question.getQuestionNo())
                : question.getQuestionNo() + "(" + selectedOption + ")";
        return new ResolvedQuestionScore(question.getQuestionNo(), max, auditKey);
    }

    private static List<TemplateSnapshotQuestion> materializeQuestions(TemplateSnapshotSection section) {
        if (section.getQuestions() != null && !section.getQuestions().isEmpty()) {
            return section.getQuestions().stream()
                    .sorted(Comparator.comparing(TemplateSnapshotQuestion::getQuestionNo))
                    .collect(Collectors.toList());
        }

        List<TemplateSnapshotQuestion> generated = new ArrayList<>();
        int total = section.getTotalQuestions() != null ? section.getTotalQuestions() : section.getQuestionCount();
        for (int i = 1; i <= total; i++) {
            generated.add(TemplateSnapshotQuestion.builder()
                    .questionNo(i)
                    .marks(section.getMarksPerQuestion())
                    .type(TemplateQuestionType.NORMAL)
                    .options(List.of())
                    .build());
        }
        return generated;
    }

    private static TemplateSectionType resolveSectionType(TemplateSnapshotSection section) {
        if (section.getSectionType() == null) {
            return TemplateSectionType.FIXED;
        }
        return section.getSectionType();
    }

    private static int resolveAttemptQuestions(TemplateSnapshotSection section) {
        int total = section.getTotalQuestions() != null ? section.getTotalQuestions() : section.getQuestionCount();
        return section.getAttemptQuestions() == null ? total : section.getAttemptQuestions();
    }

    static String entryKey(String section, Integer questionNo, String optionLabel) {
        String normalizedSection = section == null ? "" : section.trim();
        return normalizedSection + "#" + questionNo + "#" + normalizeOptionLabel(optionLabel);
    }

    static String normalizeOptionLabel(String optionLabel) {
        return optionLabel == null ? "" : optionLabel.trim().toLowerCase(Locale.ROOT);
    }

    record ScoreComputationResult(BigDecimal totalMarks,
                                  Map<String, BigDecimal> sectionTotals,
                                  Map<String, List<String>> selectedQuestions) {
    }

    record ResolvedQuestionScore(Integer questionNo, BigDecimal marks, String auditKey) {
    }
}

