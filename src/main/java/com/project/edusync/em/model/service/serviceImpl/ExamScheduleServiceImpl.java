package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.model.entity.Subject;
import com.project.edusync.adm.repository.AcademicClassRepository;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.adm.repository.SubjectRepository;
import com.project.edusync.common.config.CacheNames;
import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.common.exception.emException.ExamNotFoundException;
import com.project.edusync.em.model.dto.RequestDTO.ExamScheduleRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.EvaluationStructureResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamScheduleResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.TemplateSnapshotResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.TemplateSnapshotQuestionDTO;
import com.project.edusync.em.model.dto.ResponseDTO.TemplateSnapshotSectionDTO;
import com.project.edusync.em.model.entity.Exam;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.entity.ExamTemplate;
import com.project.edusync.em.model.entity.TemplateQuestion;
import com.project.edusync.em.model.entity.TemplateSection;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshotQuestion;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshot;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshotSection;
import com.project.edusync.em.model.enums.TemplateSectionType;
import com.project.edusync.em.model.repository.ExamRepository;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.ExamTemplateRepository;
import com.project.edusync.em.model.service.ExamScheduleService;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamScheduleServiceImpl implements ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    // Repositories for ADM entities
    private final AcademicClassRepository academicClassRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ExamTemplateRepository examTemplateRepository;
    private final com.project.edusync.adm.repository.TimeslotRepository timeslotRepository;
    private final StudentRepository studentRepository;

    private final com.project.edusync.em.model.repository.SittingPlanRepository sittingPlanRepository;
    private final com.project.edusync.em.model.repository.SeatAllocationRepository seatAllocationRepository;
    private final com.project.edusync.em.model.repository.InvigilationRepository invigilationRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ROOM_AVAILABILITY, allEntries = true),
            @CacheEvict(value = CacheNames.EXAM_TEMPLATES, key = "#requestDTO.templateId"),
            @CacheEvict(value = CacheNames.SEATING_PLAN_PDF, allEntries = true)
    })
    public ExamScheduleResponseDTO createSchedule(UUID examUuid, ExamScheduleRequestDTO requestDTO) {
        Exam exam = examRepository.findByUuid(examUuid)
                .orElseThrow(() -> new ExamNotFoundException(examUuid));

        validateRequest(requestDTO);

        ExamSchedule schedule = new ExamSchedule();
        schedule.setExam(exam);
        mapDtoToEntity(requestDTO, schedule);

        return mapEntityToResponse(examScheduleRepository.save(schedule));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ROOM_AVAILABILITY, allEntries = true),
            @CacheEvict(value = CacheNames.EXAM_TEMPLATES, key = "#requestDTO.templateId"),
            @CacheEvict(value = CacheNames.SEATING_PLAN_PDF, allEntries = true)
    })
    public ExamScheduleResponseDTO updateSchedule(Long scheduleId, ExamScheduleRequestDTO requestDTO) {
        ExamSchedule schedule = examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND));


        validateRequest(requestDTO);
        mapDtoToEntity(requestDTO, schedule);

        return mapEntityToResponse(examScheduleRepository.save(schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamScheduleResponseDTO> getSchedulesByExam(UUID examUuid) {
        Exam exam = examRepository.findByUuid(examUuid)
                .orElseThrow(() -> new ExamNotFoundException(examUuid));

        return exam.getSchedules().stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExamScheduleResponseDTO getScheduleById(Long scheduleId) {
        ExamSchedule schedule = examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND));
        return mapEntityToResponse(schedule);
    }

    @Override
    @CacheEvict(value = {CacheNames.ROOM_AVAILABILITY, CacheNames.SCHEDULE_STUDENTS, CacheNames.SEATING_PLAN_PDF}, allEntries = true)
    public void deleteSchedule(Long scheduleId) {
        if (!examScheduleRepository.existsById(scheduleId)) {
            throw new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND);
        }
        
        // Cascade delete explicit relationships
        sittingPlanRepository.deleteAllInBatch(sittingPlanRepository.findByExamScheduleId(scheduleId));
        invigilationRepository.deleteAllInBatch(invigilationRepository.findByExamScheduleId(scheduleId));
        seatAllocationRepository.deleteAllInBatch(seatAllocationRepository.findByExamScheduleId(scheduleId));

        examScheduleRepository.deleteById(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationStructureResponseDTO getEvaluationStructure(Long scheduleId) {
        ExamSchedule schedule = examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND));

        TemplateSnapshot snapshot = schedule.getTemplateSnapshot();
        if (snapshot == null || snapshot.getSections() == null || snapshot.getSections().isEmpty()) {
            throw new EdusyncException("EM-400", "Template snapshot is missing for this schedule", HttpStatus.BAD_REQUEST);
        }

        int runningQuestionNo = 1;
        List<EvaluationStructureResponseDTO.EvaluationSectionDTO> sections = new java.util.ArrayList<>();
        List<TemplateSnapshotSection> orderedSections = snapshot.getSections().stream()
                .sorted(Comparator.comparing(TemplateSnapshotSection::getSectionOrder))
                .collect(Collectors.toList());
        for (TemplateSnapshotSection section : orderedSections) {
            List<EvaluationStructureResponseDTO.EvaluationQuestionDTO> questions = new java.util.ArrayList<>();
            List<TemplateSnapshotQuestion> sectionQuestions = getSnapshotQuestions(section);
            for (TemplateSnapshotQuestion question : sectionQuestions) {
                questions.add(EvaluationStructureResponseDTO.EvaluationQuestionDTO.builder()
                        .qNo(runningQuestionNo++)
                        .maxMarks(question.getMarks())
                        .type(question.getType())
                        .options(question.getOptions())
                        .build());
            }
            sections.add(EvaluationStructureResponseDTO.EvaluationSectionDTO.builder()
                    .name(section.getName())
                    .totalQuestions(resolveTotalQuestions(section))
                    .attemptQuestions(resolveAttemptQuestions(section))
                    .sectionType(resolveSectionType(section))
                    .helperText(resolveSectionType(section) == TemplateSectionType.OPTIONAL
                            ? "Best " + resolveAttemptQuestions(section) + " answers will be considered automatically"
                            : null)
                    .questions(questions)
                    .build());
        }

        return EvaluationStructureResponseDTO.builder().sections(sections).build();
    }

    // --- Helper Methods ---

    private void validateRequest(ExamScheduleRequestDTO dto) {
        if (dto.getTemplateId() == null) {
            throw new EdusyncException("EM-400", "templateId is required", HttpStatus.BAD_REQUEST);
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new EdusyncException("EM-400", "End time must be after start time", HttpStatus.BAD_REQUEST);
        }
        if (dto.getPassingMarks() != null && dto.getPassingMarks().compareTo(dto.getMaxMarks()) > 0) {
            throw new EdusyncException("EM-400", "Passing marks cannot be greater than max marks", HttpStatus.BAD_REQUEST);
        }

        int maxStudentsPerSeat = dto.getMaxStudentsPerSeat() != null ? dto.getMaxStudentsPerSeat() : 1;
        if (maxStudentsPerSeat < 1 || maxStudentsPerSeat > 3) {
            throw new EdusyncException("EM-400", "maxStudentsPerSeat must be 1, 2, or 3", HttpStatus.BAD_REQUEST);
        }
    }

    private void mapDtoToEntity(ExamScheduleRequestDTO dto, ExamSchedule entity) {
        AcademicClass academicClass = academicClassRepository.findById(dto.getClassId())
                .orElseThrow(() -> new EdusyncException("ADM-404", "Class not found", HttpStatus.NOT_FOUND));
        entity.setAcademicClass(academicClass);

        if (dto.getSectionId() != null) {
            Section section = sectionRepository.findById(dto.getSectionId())
                    .orElseThrow(() -> new EdusyncException("ADM-404", "Section not found", HttpStatus.NOT_FOUND));
            if (!section.getAcademicClass().getId().equals(academicClass.getId())) {
                throw new EdusyncException("EM-400", "Section does not belong to selected class", HttpStatus.BAD_REQUEST);
            }
            entity.setSection(section);
        } else {
            entity.setSection(null);
        }

        Subject subject = subjectRepository.findActiveById(dto.getSubjectId())
                .orElseThrow(() -> new EdusyncException("ADM-404", "Subject not found", HttpStatus.NOT_FOUND));
        entity.setSubject(subject);

        ExamTemplate template = examTemplateRepository.findByUuidWithSections(dto.getTemplateId())
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam template not found", HttpStatus.NOT_FOUND));
        TemplateSnapshot snapshot = buildSnapshot(template);
        entity.setTemplate(template);
        entity.setTemplateSnapshot(snapshot);
        entity.setMaxMarks(snapshot.getTotalMarks());
        if (!template.isInUse()) {
            template.setInUse(true);
            examTemplateRepository.save(template);
        }

        // --- Timeslot mapping ---
        // NOTE: Timeslot requires dayOfWeek, startTime, endTime. We use examDate.getDayOfWeek().getValue() for dayOfWeek.
        java.time.LocalTime startTime = dto.getStartTime();
        java.time.LocalTime endTime = dto.getEndTime();
        Short dayOfWeek = dto.getExamDate() != null ? (short) dto.getExamDate().getDayOfWeek().getValue() : null;
        com.project.edusync.adm.model.entity.Timeslot timeslot = timeslotRepository
                .findByStartTimeAndEndTime(startTime, endTime)
                .orElseGet(() -> {
                    com.project.edusync.adm.model.entity.Timeslot ts = new com.project.edusync.adm.model.entity.Timeslot();
                    ts.setStartTime(startTime);
                    ts.setEndTime(endTime);
                    ts.setDayOfWeek(dayOfWeek);
                    ts.setIsActive(true);
                    return timeslotRepository.save(ts);
                });
        entity.setTimeslot(timeslot);

        entity.setExamDate(dto.getExamDate());
        entity.setDuration(dto.getDuration());
        if (dto.getMaxMarks() != null && dto.getMaxMarks().intValue() != snapshot.getTotalMarks()) {
            throw new EdusyncException("EM-400", "maxMarks must match selected template totalMarks", HttpStatus.BAD_REQUEST);
        }

        int maxStudentsPerSeat = dto.getMaxStudentsPerSeat() != null ? dto.getMaxStudentsPerSeat() : 1;
        entity.setMaxStudentsPerSeat(maxStudentsPerSeat);

        long activeStudents = entity.getSection() != null
                ? studentRepository.countBySection_IdAndIsActiveTrue(entity.getSection().getId())
                : studentRepository.countBySection_AcademicClass_IdAndIsActiveTrue(entity.getAcademicClass().getId());
        entity.setActiveStudentCount(Math.toIntExact(activeStudents));
    }

    private ExamScheduleResponseDTO mapEntityToResponse(ExamSchedule entity) {
        long totalStudents = entity.getActiveStudentCount() != null
                ? entity.getActiveStudentCount()
                : 0;

        return ExamScheduleResponseDTO.builder()
                .scheduleId(entity.getId())
                .examUuid(entity.getExam().getUuid())
                .templateId(entity.getTemplate() != null ? entity.getTemplate().getUuid() : null)
                .templateSnapshot(toSnapshotResponse(entity.getTemplateSnapshot()))
                .classId(entity.getAcademicClass().getUuid())
                .className(entity.getAcademicClass().getName())
                .sectionId(entity.getSection() != null ? entity.getSection().getUuid() : null)
                .sectionName(entity.getSection() != null ? entity.getSection().getSectionName() : null)
                .subjectId(entity.getSubject().getUuid())
                .subjectName(entity.getSubject().getName())
                .examDate(entity.getExamDate())
                .startTime(entity.getTimeslot() != null ? entity.getTimeslot().getStartTime() : null)
                .endTime(entity.getTimeslot() != null ? entity.getTimeslot().getEndTime() : null)
                .maxMarks(java.math.BigDecimal.valueOf(entity.getMaxMarks()))
                .passingMarks(java.math.BigDecimal.valueOf(entity.getMaxMarks())) // TODO: replace with actual field if available
                .totalStudents(totalStudents)
                .maxStudentsPerSeat(entity.getMaxStudentsPerSeat())
                .build();
    }

    private TemplateSnapshot buildSnapshot(ExamTemplate template) {
        List<TemplateSnapshotSection> sections = template.getSections().stream()
                .sorted(Comparator.comparing(TemplateSection::getSectionOrder))
                .map(section -> TemplateSnapshotSection.builder()
                        .name(section.getSectionName())
                        .sectionOrder(section.getSectionOrder())
                        .questionCount(section.getQuestionCount())
                        .marksPerQuestion(section.getMarksPerQuestion())
                        .totalQuestions(section.getTotalQuestions() != null ? section.getTotalQuestions() : section.getQuestionCount())
                        .attemptQuestions(section.getAttemptQuestions() != null
                                ? section.getAttemptQuestions()
                                : (section.getTotalQuestions() != null ? section.getTotalQuestions() : section.getQuestionCount()))
                        .sectionType(normalizeSectionType(section.getSectionType()))
                        .internalChoiceEnabled(Boolean.TRUE.equals(section.getInternalChoiceEnabled()))
                        .questions((section.getQuestions() == null ? List.<TemplateQuestion>of() : section.getQuestions()).stream()
                                .sorted(Comparator.comparing(TemplateQuestion::getQuestionNo))
                                .map(question -> TemplateSnapshotQuestion.builder()
                                        .questionNo(question.getQuestionNo())
                                        .marks(question.getMarks())
                                        .type(question.getType())
                                        .options((question.getOptions() == null ? List.<String>of() : question.getOptions()).stream()
                                                .sorted(String::compareToIgnoreCase)
                                                .collect(Collectors.toList()))
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return TemplateSnapshot.builder()
                .templateName(template.getName())
                .totalMarks(template.getTotalMarks())
                .totalQuestions(template.getTotalQuestions())
                .sections(sections)
                .build();
    }

    private TemplateSnapshotResponseDTO toSnapshotResponse(TemplateSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return TemplateSnapshotResponseDTO.builder()
                .templateName(snapshot.getTemplateName())
                .totalMarks(snapshot.getTotalMarks())
                .totalQuestions(snapshot.getTotalQuestions())
                .sections(snapshot.getSections() == null ? List.of() : snapshot.getSections().stream()
                        .sorted(Comparator.comparing(TemplateSnapshotSection::getSectionOrder))
                        .map(section -> TemplateSnapshotSectionDTO.builder()
                                .name(section.getName())
                                .sectionOrder(section.getSectionOrder())
                                .questionCount(section.getQuestionCount())
                                .marksPerQuestion(section.getMarksPerQuestion())
                                .totalQuestions(resolveTotalQuestions(section))
                                .attemptQuestions(resolveAttemptQuestions(section))
                                .sectionType(resolveSectionType(section))
                                .internalChoiceEnabled(Boolean.TRUE.equals(section.getInternalChoiceEnabled()))
                                .questions(getSnapshotQuestions(section).stream()
                                        .map(q -> TemplateSnapshotQuestionDTO.builder()
                                                .questionNo(q.getQuestionNo())
                                                .marks(q.getMarks())
                                                .type(q.getType())
                                                .options(q.getOptions())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private List<TemplateSnapshotQuestion> getSnapshotQuestions(TemplateSnapshotSection section) {
        if (section.getQuestions() != null && !section.getQuestions().isEmpty()) {
            return section.getQuestions();
        }
        List<TemplateSnapshotQuestion> generated = new java.util.ArrayList<>();
        int totalQuestions = resolveTotalQuestions(section);
        for (int i = 1; i <= totalQuestions; i++) {
            generated.add(TemplateSnapshotQuestion.builder()
                    .questionNo(i)
                    .marks(section.getMarksPerQuestion())
                    .type(com.project.edusync.em.model.enums.TemplateQuestionType.NORMAL)
                    .options(List.of())
                    .build());
        }
        return generated;
    }

    private int resolveTotalQuestions(TemplateSnapshotSection section) {
        return section.getTotalQuestions() != null ? section.getTotalQuestions() : section.getQuestionCount();
    }

    private int resolveAttemptQuestions(TemplateSnapshotSection section) {
        return section.getAttemptQuestions() != null ? section.getAttemptQuestions() : resolveTotalQuestions(section);
    }

    private TemplateSectionType resolveSectionType(TemplateSnapshotSection section) {
        return normalizeSectionType(section.getSectionType());
    }

    private TemplateSectionType normalizeSectionType(TemplateSectionType raw) {
        if (raw == null) {
            return TemplateSectionType.FIXED;
        }
        return raw;
    }
}