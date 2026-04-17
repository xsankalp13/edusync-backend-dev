package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.common.config.CacheNames;
import com.project.edusync.em.model.dto.RequestDTO.ExamTemplateRequestDTO;
import com.project.edusync.em.model.dto.RequestDTO.TemplateQuestionRequestDTO;
import com.project.edusync.em.model.dto.RequestDTO.TemplateSectionRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.EvaluationStructureResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamTemplateResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.TemplateSectionResponseDTO;
import com.project.edusync.em.model.entity.ExamTemplate;
import com.project.edusync.em.model.entity.TemplateQuestion;
import com.project.edusync.em.model.entity.TemplateSection;
import com.project.edusync.em.model.enums.TemplateQuestionType;
import com.project.edusync.em.model.enums.TemplateSectionType;
import com.project.edusync.em.model.repository.ExamTemplateRepository;
import com.project.edusync.em.model.repository.TemplateSectionRepository;
import com.project.edusync.em.model.service.ExamTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExamTemplateServiceImpl implements ExamTemplateService {

    private final ExamTemplateRepository examTemplateRepository;
    private final TemplateSectionRepository templateSectionRepository;

    @Override
    public ExamTemplateResponseDTO createTemplate(ExamTemplateRequestDTO requestDTO) {
        validateTemplateRequest(requestDTO);

        ExamTemplate template = new ExamTemplate();
        template.setName(requestDTO.getName().trim());
        template.setInUse(false);
        applySections(template, requestDTO.getSections());

        ExamTemplate saved = examTemplateRepository.save(template);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamTemplateResponseDTO> getAllTemplates() {
        return examTemplateRepository.findAllWithSections().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_TEMPLATES, key = "#templateId")
    public ExamTemplateResponseDTO getTemplateById(UUID templateId) {
        return toResponse(getTemplateEntityWithSections(templateId));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.EXAM_TEMPLATES, key = "#templateId")
    public ExamTemplateResponseDTO updateTemplate(UUID templateId, ExamTemplateRequestDTO requestDTO) {
        validateTemplateRequest(requestDTO);

        ExamTemplate template = getTemplateEntityWithSections(templateId);
        assertMutable(template);

        template.setName(requestDTO.getName().trim());

        // Full-replacement semantics: remove all old rows before inserting new sections.
        int existingSectionCount = template.getSections() == null ? 0 : template.getSections().size();
        log.info("Template update replacement start: templateId={}, existingSections={}", template.getId(), existingSectionCount);
        int deletedCount = templateSectionRepository.deleteByTemplateId(template.getId());
        templateSectionRepository.flush();
        log.info("Template update replacement delete complete: templateId={}, deletedSections={}", template.getId(), deletedCount);
        template.setSections(new ArrayList<>());

        applySections(template, requestDTO.getSections());

        try {
            return toResponse(examTemplateRepository.saveAndFlush(template));
        } catch (DataIntegrityViolationException ex) {
            log.error("Template update conflict: templateId={}, reason={}", template.getId(), ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
            throw new EdusyncException("EM-409", "Template update conflicts with existing section/question constraints", HttpStatus.CONFLICT, ex);
        }
    }

    @Override
    @CacheEvict(value = CacheNames.EXAM_TEMPLATES, key = "#templateId")
    public void deleteTemplate(UUID templateId) {
        ExamTemplate template = getTemplateEntityWithSections(templateId);
        assertMutable(template);
        examTemplateRepository.delete(template);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationStructureResponseDTO getTemplatePreview(UUID templateId) {
        ExamTemplate template = getTemplateEntityWithSections(templateId);

        int runningQuestionNo = 1;
        List<EvaluationStructureResponseDTO.EvaluationSectionDTO> sections = new ArrayList<>();
        for (TemplateSection section : orderedSections(template.getSections())) {
            List<EvaluationStructureResponseDTO.EvaluationQuestionDTO> questions = new ArrayList<>();
            List<TemplateQuestion> sectionQuestions = getSectionQuestions(section);
            for (TemplateQuestion question : sectionQuestions) {
                questions.add(EvaluationStructureResponseDTO.EvaluationQuestionDTO.builder()
                        .qNo(runningQuestionNo++)
                        .maxMarks(question.getMarks())
                        .type(question.getType())
                        .options(question.getOptions().stream().sorted().collect(Collectors.toList()))
                        .build());
            }

            sections.add(EvaluationStructureResponseDTO.EvaluationSectionDTO.builder()
                    .name(section.getSectionName())
                    .totalQuestions(resolveTotalQuestions(section))
                    .attemptQuestions(resolveAttemptQuestions(section))
                    .sectionType(resolveSectionType(section))
                    .helperText(resolveSectionType(section) == TemplateSectionType.OPTIONAL
                            ? "Best " + resolveAttemptQuestions(section) + " answers will be considered automatically"
                            : null)
                    .questions(questions)
                    .build());
        }

        return EvaluationStructureResponseDTO.builder()
                .sections(sections)
                .build();
    }

    private ExamTemplate getTemplateEntityWithSections(UUID templateId) {
        return examTemplateRepository.findByUuidWithSections(templateId)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam template not found", HttpStatus.NOT_FOUND));
    }

    private void assertMutable(ExamTemplate template) {
        if (template.isInUse()) {
            throw new EdusyncException("EM-409", "Template is already in use and cannot be modified", HttpStatus.CONFLICT);
        }
    }

    private void validateTemplateRequest(ExamTemplateRequestDTO requestDTO) {
        if (requestDTO.getSections() == null || requestDTO.getSections().isEmpty()) {
            throw new EdusyncException("EM-400", "At least one section is required", HttpStatus.BAD_REQUEST);
        }

        Set<String> normalizedSectionNames = new HashSet<>();
        for (TemplateSectionRequestDTO section : requestDTO.getSections()) {
            String normalizedName = section.getSectionName() == null
                    ? null
                    : section.getSectionName().trim().toLowerCase(Locale.ROOT);
            if (normalizedName == null || normalizedName.isEmpty()) {
                throw new EdusyncException("EM-400", "Section name is required", HttpStatus.BAD_REQUEST);
            }
            if (!normalizedSectionNames.add(normalizedName)) {
                throw new EdusyncException("EM-400", "Duplicate section name: " + section.getSectionName().trim(), HttpStatus.BAD_REQUEST);
            }

            int resolvedTotalQuestions = resolveTotalQuestions(section);
            int resolvedAttemptQuestions = resolveAttemptQuestions(section, resolvedTotalQuestions);
            if (resolvedAttemptQuestions > resolvedTotalQuestions) {
                throw new EdusyncException("EM-400", "attemptQuestions cannot be greater than totalQuestions", HttpStatus.BAD_REQUEST);
            }
            TemplateSectionType sectionType = normalizeSectionType(section.getSectionType());
            if (sectionType == TemplateSectionType.OPTIONAL && section.getAttemptQuestions() == null) {
                throw new EdusyncException("EM-400", "attemptQuestions must be provided for OPTIONAL sections", HttpStatus.BAD_REQUEST);
            }

            List<TemplateQuestionRequestDTO> questions = section.getQuestions();
            if (questions != null && !questions.isEmpty()) {
                if (questions.size() != resolvedTotalQuestions) {
                    throw new EdusyncException("EM-400", "questions size must match totalQuestions", HttpStatus.BAD_REQUEST);
                }
                Set<Integer> questionNos = new HashSet<>();
                for (TemplateQuestionRequestDTO question : questions) {
                    if (!questionNos.add(question.getQuestionNo())) {
                        throw new EdusyncException("EM-400", "Duplicate questionNo in section " + section.getSectionName(), HttpStatus.BAD_REQUEST);
                    }
                    TemplateQuestionType type = question.getType() == null ? TemplateQuestionType.NORMAL : question.getType();
                    if (type == TemplateQuestionType.INTERNAL_CHOICE) {
                        if (question.getOptions() == null || question.getOptions().size() < 2) {
                            throw new EdusyncException("EM-400", "INTERNAL_CHOICE question must have at least 2 options", HttpStatus.BAD_REQUEST);
                        }
                    }
                }
            } else {
                if (section.getMarksPerQuestion() == null || section.getMarksPerQuestion() < 1) {
                    throw new EdusyncException("EM-400", "marksPerQuestion must be at least 1 when explicit questions are not provided", HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    private void applySections(ExamTemplate template, List<TemplateSectionRequestDTO> sectionRequests) {
        List<TemplateSection> sections = new ArrayList<>();
        for (int i = 0; i < sectionRequests.size(); i++) {
            TemplateSectionRequestDTO section = sectionRequests.get(i);
            int totalQuestions = resolveTotalQuestions(section);
            int attemptQuestions = resolveAttemptQuestions(section, totalQuestions);
            TemplateSectionType sectionType = normalizeSectionType(section.getSectionType());
            TemplateSection built = TemplateSection.builder()
                        .template(template)
                        .sectionName(section.getSectionName().trim())
                        .sectionOrder(i + 1)
                        .questionCount(totalQuestions)
                        .totalQuestions(totalQuestions)
                        .attemptQuestions(attemptQuestions)
                        .sectionType(sectionType)
                        .internalChoiceEnabled(resolveInternalChoiceEnabled(section))
                        .marksPerQuestion(resolveMarksPerQuestion(section))
                        .isObjective(section.getIsObjective())
                        .isSubjective(section.getIsSubjective())
                        .build();
            built.setQuestions(buildTemplateQuestions(built, section));
            log.info("Template section insert: templateId={}, sectionOrder={}, sectionType={}, attemptQuestions={}, totalQuestions={}",
                    template.getId(), built.getSectionOrder(), built.getSectionType(), built.getAttemptQuestions(), built.getTotalQuestions());
            sections.add(built);
        }

        int totalMarks = sections.stream()
                .mapToInt(this::resolveSectionMaxMarks)
                .sum();
        int totalQuestions = sections.stream()
                .mapToInt(section -> resolveTotalQuestions(section))
                .sum();

        template.getSections().addAll(sections);
        template.setTotalMarks(totalMarks);
        template.setTotalQuestions(totalQuestions);
    }

    private List<TemplateSection> orderedSections(List<TemplateSection> sections) {
        return sections.stream()
                .sorted(Comparator.comparing(TemplateSection::getSectionOrder))
                .collect(Collectors.toList());
    }

    private ExamTemplateResponseDTO toResponse(ExamTemplate template) {
        return ExamTemplateResponseDTO.builder()
                .id(template.getUuid())
                .name(template.getName())
                .totalMarks(template.getTotalMarks())
                .totalQuestions(template.getTotalQuestions())
                .inUse(template.isInUse())
                .createdAt(template.getCreatedAt())
                .sections(orderedSections(template.getSections()).stream()
                        .map(section -> TemplateSectionResponseDTO.builder()
                                .id(section.getUuid())
                                .sectionName(section.getSectionName())
                                .sectionOrder(section.getSectionOrder())
                                .questionCount(section.getQuestionCount())
                                .totalQuestions(resolveTotalQuestions(section))
                                .attemptQuestions(resolveAttemptQuestions(section))
                                .sectionType(resolveSectionType(section))
                                .internalChoiceEnabled(Boolean.TRUE.equals(section.getInternalChoiceEnabled()))
                                .marksPerQuestion(section.getMarksPerQuestion())
                                .isObjective(section.getIsObjective())
                                .isSubjective(section.getIsSubjective())
                                .questions(getSectionQuestions(section).stream()
                                        .sorted(Comparator.comparing(TemplateQuestion::getQuestionNo))
                                        .map(question -> com.project.edusync.em.model.dto.ResponseDTO.TemplateQuestionResponseDTO.builder()
                                                .id(question.getUuid())
                                                .questionNo(question.getQuestionNo())
                                                .marks(question.getMarks())
                                                .type(question.getType())
                                                .options(question.getOptions().stream().sorted().collect(Collectors.toList()))
                                                .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private int resolveTotalQuestions(TemplateSectionRequestDTO section) {
        Integer total = section.getTotalQuestions() != null ? section.getTotalQuestions() : section.getQuestionCount();
        if (total == null || total < 1) {
            throw new EdusyncException("EM-400", "totalQuestions must be at least 1", HttpStatus.BAD_REQUEST);
        }
        return total;
    }

    private int resolveAttemptQuestions(TemplateSectionRequestDTO section, int totalQuestions) {
        if (normalizeSectionType(section.getSectionType()) == TemplateSectionType.OPTIONAL) {
            if (section.getAttemptQuestions() == null || section.getAttemptQuestions() < 1) {
                throw new EdusyncException("EM-400", "attemptQuestions must be provided for OPTIONAL sections", HttpStatus.BAD_REQUEST);
            }
            return section.getAttemptQuestions();
        }
        return totalQuestions;
    }

    private boolean resolveInternalChoiceEnabled(TemplateSectionRequestDTO section) {
        if (section.getInternalChoiceEnabled() != null) {
            return section.getInternalChoiceEnabled();
        }
        if (section.getQuestions() == null || section.getQuestions().isEmpty()) {
            return false;
        }
        return section.getQuestions().stream()
                .anyMatch(question -> (question.getType() == null ? TemplateQuestionType.NORMAL : question.getType()) == TemplateQuestionType.INTERNAL_CHOICE);
    }

    private int resolveMarksPerQuestion(TemplateSectionRequestDTO section) {
        if (section.getMarksPerQuestion() != null && section.getMarksPerQuestion() > 0) {
            return section.getMarksPerQuestion();
        }
        if (section.getQuestions() != null && !section.getQuestions().isEmpty()) {
            return section.getQuestions().stream().findFirst().map(TemplateQuestionRequestDTO::getMarks).orElse(1);
        }
        throw new EdusyncException("EM-400", "marksPerQuestion is required", HttpStatus.BAD_REQUEST);
    }

    private List<TemplateQuestion> buildTemplateQuestions(TemplateSection section, TemplateSectionRequestDTO request) {
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            return request.getQuestions().stream()
                    .sorted(Comparator.comparing(TemplateQuestionRequestDTO::getQuestionNo))
                    .map(q -> {
                        TemplateQuestion question = TemplateQuestion.builder()
                                .section(section)
                                .questionNo(q.getQuestionNo())
                                .marks(q.getMarks())
                                .type(q.getType() == null ? TemplateQuestionType.NORMAL : q.getType())
                                .build();
                        List<String> options = (q.getOptions() == null ? List.<String>of() : q.getOptions()).stream()
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .distinct()
                                .sorted(String::compareToIgnoreCase)
                                .collect(Collectors.toList());
                        question.setOptions(options);
                        return question;
                    })
                    .collect(Collectors.toList());
        }

        List<TemplateQuestion> generated = new ArrayList<>();
        int totalQuestions = resolveTotalQuestions(request);
        for (int i = 1; i <= totalQuestions; i++) {
            generated.add(TemplateQuestion.builder()
                    .section(section)
                    .questionNo(i)
                    .marks(resolveMarksPerQuestion(request))
                    .type(TemplateQuestionType.NORMAL)
                    .build());
        }
        return generated;
    }

    private int resolveTotalQuestions(TemplateSection section) {
        return section.getTotalQuestions() != null ? section.getTotalQuestions() : section.getQuestionCount();
    }

    private int resolveAttemptQuestions(TemplateSection section) {
        return section.getAttemptQuestions() != null ? section.getAttemptQuestions() : resolveTotalQuestions(section);
    }

    private TemplateSectionType resolveSectionType(TemplateSection section) {
        return normalizeSectionType(section.getSectionType());
    }

    private TemplateSectionType normalizeSectionType(TemplateSectionType raw) {
        if (raw == null) {
            return TemplateSectionType.FIXED;
        }
        return raw;
    }

    private int resolveSectionMaxMarks(TemplateSection section) {
        List<Integer> marks = getSectionQuestions(section).stream()
                .map(TemplateQuestion::getMarks)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        if (resolveSectionType(section) == TemplateSectionType.OPTIONAL) {
            return marks.stream().limit(resolveAttemptQuestions(section)).mapToInt(Integer::intValue).sum();
        }
        return marks.stream().mapToInt(Integer::intValue).sum();
    }

    private List<TemplateQuestion> getSectionQuestions(TemplateSection section) {
        if (section.getQuestions() != null && !section.getQuestions().isEmpty()) {
            return section.getQuestions().stream()
                    .sorted(Comparator.comparing(TemplateQuestion::getQuestionNo))
                    .collect(Collectors.toList());
        }
        List<TemplateQuestion> generated = new ArrayList<>();
        int totalQuestions = resolveTotalQuestions(section);
        for (int i = 1; i <= totalQuestions; i++) {
            generated.add(TemplateQuestion.builder()
                    .section(section)
                    .questionNo(i)
                    .marks(section.getMarksPerQuestion())
                    .type(TemplateQuestionType.NORMAL)
                    .build());
        }
        return generated;
    }
}

