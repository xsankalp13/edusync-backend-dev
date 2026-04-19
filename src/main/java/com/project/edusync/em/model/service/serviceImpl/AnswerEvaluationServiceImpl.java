package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.config.CacheNames;
import com.project.edusync.em.model.dto.RequestDTO.AnnotationRequestDTO;
import com.project.edusync.em.model.dto.RequestDTO.EvaluationAssignmentCreateRequestDTO;
import com.project.edusync.em.model.dto.RequestDTO.SaveEvaluationMarksRequestDTO;
import com.project.edusync.em.model.dto.RequestDTO.SaveQuestionMarkRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.*;
import com.project.edusync.em.model.entity.*;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshot;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshotQuestion;
import com.project.edusync.em.model.entity.snapshot.TemplateSnapshotSection;
import com.project.edusync.em.model.enums.AnnotationType;
import com.project.edusync.em.model.enums.AnswerSheetStatus;
import com.project.edusync.em.model.enums.EvaluationAssignmentRole;
import com.project.edusync.em.model.enums.EvaluationAssignmentStatus;
import com.project.edusync.em.model.enums.EvaluationAuditEventType;
import com.project.edusync.em.model.enums.EvaluationResultStatus;
import com.project.edusync.em.model.enums.TemplateQuestionType;
import com.project.edusync.em.model.enums.TemplateSectionType;
import com.project.edusync.em.model.enums.UploadStatus;
import com.project.edusync.em.model.repository.*;
import com.project.edusync.em.model.service.AnswerEvaluationService;
import com.project.edusync.em.model.service.EvaluationAuditService;
import com.project.edusync.em.model.service.EvaluationDraftStoreService;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.edusync.uis.config.MediaUploadProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AnswerEvaluationServiceImpl implements AnswerEvaluationService {

    private static final long PDF_MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final long IMAGE_MAX_SIZE_BYTES_DEFAULT = 5L * 1024 * 1024;
    private static final String PDF_MAGIC = "%PDF-";
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");
    private static final Set<String> ALLOWED_METADATA_KEYS = Set.of("color", "strokeWidth", "path");

    private final EvaluationAssignmentRepository assignmentRepository;
    private final AnswerSheetRepository answerSheetRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final QuestionMarkRepository questionMarkRepository;
    private final AnswerSheetAnnotationRepository annotationRepository;
    private final AnswerSheetImageRepository answerSheetImageRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final StudentExamStatusRepository studentExamStatusRepository;
    private final AuthUtil authUtil;
    private final EvaluationAuditService evaluationAuditService;
    private final EvaluationDraftStoreService evaluationDraftStoreService;
    private final PdfGenerationService pdfGenerationService;

    @Value("${app.evaluation.storage.private-dir:uploads-private/answer-sheets}")
    private String privateStorageDir;

    @Value("${app.evaluation.upload.max-per-minute:20}")
    private int maxUploadsPerMinute;

    @Value("${app.evaluation.image.max-size-bytes:5242880}")
    private long maxImageSizeBytes;

    @Value("${app.evaluation.file-signing-secret:${app.jwt.secret-key}}")
    private String fileSigningSecret;

    @Value("${api.url:/api/v1}")
    private String apiUrl;

    private Cloudinary cloudinary;
    private final MediaUploadProperties mediaUploadProperties;

    @jakarta.annotation.PostConstruct
    private void initCloudinary() {
        MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cfg.getCloudName(),
                "api_key", cfg.getApiKey(),
                "api_secret", cfg.getApiSecret()
        ));
    }

    private final Map<Long, Deque<Long>> uploadWindow = new ConcurrentHashMap<>();
    private final Map<Long, Object> draftSaveLocks = new ConcurrentHashMap<>();

    @Override
    public EvaluationAssignmentResponseDTO assignTeacher(EvaluationAssignmentCreateRequestDTO request) {
        requireAdmin();
        ExamSchedule schedule = getSchedule(request.getExamScheduleId());
        Staff teacher = staffRepository.findByUuid(request.getTeacherId())
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Teacher not found", HttpStatus.NOT_FOUND));

        EvaluationAssignmentRole role = request.getRole() != null
                ? request.getRole()
                : EvaluationAssignmentRole.EVALUATOR;

        EvaluationAssignment assignment = assignmentRepository
                .findByExamScheduleIdAndTeacherIdAndRole(schedule.getId(), teacher.getId(), role)
                .orElseGet(EvaluationAssignment::new);
        assignment.setExamSchedule(schedule);
        assignment.setTeacher(teacher);
        assignment.setRole(role);
        assignment.setDueDate(request.getDueDate());
        assignment.setStatus(EvaluationAssignmentStatus.ASSIGNED);

        if (role == EvaluationAssignmentRole.UPLOADER && assignment.getUploadStatus() == null) {
            assignment.setUploadStatus(UploadStatus.NOT_STARTED);
        }

        EvaluationAssignment saved = assignmentRepository.save(assignment);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("assignedByUserId", authUtil.getCurrentUserId());
        metadata.put("scheduleId", schedule.getId());
        metadata.put("teacherId", teacher.getId());
        metadata.put("role", saved.getRole().name());
        metadata.put("status", saved.getStatus().name());
        metadata.put("dueDate", saved.getDueDate() != null ? saved.getDueDate().toString() : null);
        evaluationAuditService.record(EvaluationAuditEventType.ASSIGNMENT_UPSERTED, null, saved, null, null, metadata);
        log.info("Evaluation assignment created/updated: scheduleId={}, teacherId={}, role={}",
                schedule.getId(), teacher.getId(), role);
        return toAssignmentResponse(saved);
    }

    @Override
    @Transactional
    public EvaluationAssignmentResponseDTO markScheduleUploadComplete(Long scheduleId) {
        Staff teacher = getCurrentTeacher();
        EvaluationAssignment uploaderAssignment = assignmentRepository
                .findByExamScheduleIdAndTeacherIdAndRole(scheduleId, teacher.getId(), EvaluationAssignmentRole.UPLOADER)
                .orElseThrow(() -> new EdusyncException("EVAL-403",
                        "You are not assigned as uploader for this schedule", HttpStatus.FORBIDDEN));
        uploaderAssignment.setUploadStatus(UploadStatus.COMPLETED);
        EvaluationAssignment saved = assignmentRepository.save(uploaderAssignment);
        log.info("Uploader marked upload complete: scheduleId={}, teacherId={}", scheduleId, teacher.getId());
        return toAssignmentResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAssignment(Long assignmentId) {
        requireAdmin();
        EvaluationAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Assignment not found", HttpStatus.NOT_FOUND));
        assignmentRepository.delete(assignment);
        log.info("Admin deleted evaluation assignment: id={}", assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationAssignmentResponseDTO> getAssignmentsForAdmin(UUID teacherIdFilter) {
        requireAdmin();
        Long teacherId = null;
        if (teacherIdFilter != null) {
            teacherId = staffRepository.findByUuid(teacherIdFilter)
                    .orElseThrow(() -> new EdusyncException("EVAL-404", "Teacher not found", HttpStatus.NOT_FOUND))
                    .getId();
        }
        return assignmentRepository.findAllWithSchedule(teacherId).stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationAssignmentResponseDTO> getAssignmentsForCurrentTeacher() {
        Long teacherId = getCurrentTeacher().getId();
        return assignmentRepository.findAllByTeacherIdWithSchedule(teacherId).stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SCHEDULE_STUDENTS,
            key = "#scheduleId + ':' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public List<TeacherEvaluationStudentResponseDTO> getStudentsForAssignedSchedule(Long scheduleId) {
        Staff teacher = getCurrentTeacher();
        ensureTeacherAssigned(scheduleId, teacher.getId());
        ExamSchedule schedule = getSchedule(scheduleId);

        List<Student> students = schedule.getSection() != null
                ? studentRepository.findBySectionIdOrderByRollNoAsc(schedule.getSection().getId())
                : studentRepository.findBySection_AcademicClass_IdOrderByRollNoAsc(schedule.getAcademicClass().getId());

        Map<Long, AnswerSheet> answerSheetByStudentId = answerSheetRepository.findByExamScheduleId(scheduleId).stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a, (a, b) -> a));

        return students.stream()
                .map(student -> toTeacherEvaluationStudentResponse(student, answerSheetByStudentId.get(student.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherEvaluationStudentResponseDTO> getStudentsForAssignedSchedule(Long scheduleId, int page, int size) {
        Staff teacher = getCurrentTeacher();
        ensureTeacherAssigned(scheduleId, teacher.getId());
        ExamSchedule schedule = getSchedule(scheduleId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(Math.min(size, 100), 1);
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        Page<Student> students = schedule.getSection() != null
                ? studentRepository.findBySectionIdOrderByRollNoAsc(schedule.getSection().getId(), pageable)
                : studentRepository.findBySection_AcademicClass_IdOrderByRollNoAsc(schedule.getAcademicClass().getId(), pageable);

        Map<Long, AnswerSheet> answerSheetByStudentId = answerSheetRepository.findByExamScheduleId(scheduleId).stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a, (a, b) -> a));

        List<TeacherEvaluationStudentResponseDTO> content = students.getContent().stream()
                .map(student -> toTeacherEvaluationStudentResponse(student, answerSheetByStudentId.get(student.getId())))
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, students.getTotalElements());
    }

    @Override
    @CacheEvict(value = CacheNames.SCHEDULE_STUDENTS, allEntries = true)
    public AnswerSheetUploadResponseDTO uploadAnswerSheet(Long scheduleId, UUID studentId, MultipartFile file) {
        Staff teacher = getCurrentTeacher();
        ensureTeacherCanUpload(scheduleId, teacher.getId());
        enforceUploadRateLimit(teacher.getId());

        ExamSchedule schedule = getSchedule(scheduleId);
        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Student not found", HttpStatus.NOT_FOUND));

        if (schedule.getSection() != null && !Objects.equals(student.getSection().getId(), schedule.getSection().getId())) {
            throw new EdusyncException("EVAL-400", "Student does not belong to assigned section", HttpStatus.BAD_REQUEST);
        }
        if (schedule.getSection() == null && !Objects.equals(student.getSection().getAcademicClass().getId(), schedule.getAcademicClass().getId())) {
            throw new EdusyncException("EVAL-400", "Student does not belong to assigned class", HttpStatus.BAD_REQUEST);
        }

        validatePdfUpload(file);

        String fileUrl;
        try {
            MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
            String folder = cfg.getFolder() != null ? cfg.getFolder() : "answer-sheets";
            
            String originalName = file.getOriginalFilename();
            String nameWithoutExtension = originalName != null && originalName.contains(".") 
                ? originalName.substring(0, originalName.lastIndexOf('.')) 
                : (originalName != null ? originalName : "file");

            String publicId = folder + "/" + UUID.randomUUID() + "_" + nameWithoutExtension;
            
            var uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "raw",
                    "flags", "attachment" 
            ));
            fileUrl = (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw new EdusyncException("EVAL-500", "Failed to upload answer sheet to storage", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        AnswerSheet answerSheet = answerSheetRepository.findByExamScheduleIdAndStudentId(scheduleId, student.getId())
                .orElseGet(AnswerSheet::new);
        answerSheet.setExamSchedule(schedule);
        answerSheet.setStudent(student);
        answerSheet.setUploadedByTeacher(teacher);
        answerSheet.setFileUrl(fileUrl);
        answerSheet.setStatus(AnswerSheetStatus.UPLOADED);

        AnswerSheet saved = answerSheetRepository.save(answerSheet);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scheduleId", schedule.getId());
        metadata.put("studentId", student.getId());
        metadata.put("answerSheetStatus", saved.getStatus().name());
        evaluationAuditService.record(EvaluationAuditEventType.ANSWER_SHEET_UPLOADED, teacher, null, saved, null, metadata);
        String signedUrl = generateSignedFileUrl(saved.getId());
        log.info("Answer sheet uploaded: answerSheetId={}, scheduleId={}, studentId={}", saved.getId(), scheduleId, student.getId());

        return AnswerSheetUploadResponseDTO.builder()
                .answerSheetId(saved.getId())
                .fileUrl(signedUrl)
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @CacheEvict(value = CacheNames.SCHEDULE_STUDENTS, allEntries = true)
    public AnswerSheetImageGroupResponseDTO uploadAnswerSheetImages(Long scheduleId,
                                                                     UUID studentId,
                                                                     List<MultipartFile> files,
                                                                     List<Integer> pageNumbers) {
        Staff teacher = getCurrentTeacher();
        ensureTeacherCanUpload(scheduleId, teacher.getId());
        enforceUploadRateLimit(teacher.getId());

        if (files == null || files.isEmpty()) {
            throw new EdusyncException("EVAL-400", "At least one image is required", HttpStatus.BAD_REQUEST);
        }

        ExamSchedule schedule = getSchedule(scheduleId);
        Student student = getValidatedStudentForSchedule(studentId, schedule);
        List<Integer> resolvedPages = resolvePageNumbers(pageNumbers, files.size());

        AnswerSheet answerSheet = answerSheetRepository.findByExamScheduleIdAndStudentId(scheduleId, student.getId())
                .orElseGet(AnswerSheet::new);
        if (answerSheet.getId() != null && !Objects.equals(answerSheet.getUploadedByTeacher().getId(), teacher.getId())) {
            throw new EdusyncException("EVAL-403", "Only the assigned uploader can update these images", HttpStatus.FORBIDDEN);
        }

        answerSheet.setExamSchedule(schedule);
        answerSheet.setStudent(student);
        answerSheet.setUploadedByTeacher(teacher);
        answerSheet.setStatus(AnswerSheetStatus.UPLOADED);
        AnswerSheet savedSheet = answerSheetRepository.save(answerSheet);

        Map<Integer, AnswerSheetImage> existingByPage = answerSheetImageRepository
                .findByAnswerSheetIdOrderByPageNumberAsc(savedSheet.getId())
                .stream()
                .collect(Collectors.toMap(AnswerSheetImage::getPageNumber, image -> image, (left, right) -> left, LinkedHashMap::new));

        List<AnswerSheetImage> toSave = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            Integer pageNumber = resolvedPages.get(i);
            validateImageUpload(file, pageNumber);
            String imageUrl = uploadImageToCloudinary(file, savedSheet.getId(), pageNumber);

            AnswerSheetImage image = existingByPage.getOrDefault(pageNumber, AnswerSheetImage.builder()
                    .answerSheet(savedSheet)
                    .pageNumber(pageNumber)
                    .build());
            image.setImageUrl(imageUrl);
            toSave.add(image);
        }

        answerSheetImageRepository.saveAll(toSave);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scheduleId", scheduleId);
        metadata.put("studentId", student.getId());
        metadata.put("uploadedPages", resolvedPages.size());
        evaluationAuditService.record(EvaluationAuditEventType.ANSWER_SHEET_UPLOADED, teacher, null, savedSheet, null, metadata);
        return toImageGroupResponse(savedSheet);
    }

    @Override
    @Transactional(readOnly = true)
    public AnswerSheetImageGroupResponseDTO getAnswerSheetImages(UUID studentId, Long scheduleId) {
        Staff teacher = getCurrentTeacher();
        ensureTeacherAssigned(scheduleId, teacher.getId());

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Student not found", HttpStatus.NOT_FOUND));
        AnswerSheet answerSheet = answerSheetRepository.findByExamScheduleIdAndStudentId(scheduleId, student.getId())
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Answer sheet images not found", HttpStatus.NOT_FOUND));
        return toImageGroupResponse(answerSheet);
    }

    @Override
    @CacheEvict(value = CacheNames.SCHEDULE_STUDENTS, allEntries = true)
    public AnswerSheetImageGroupResponseDTO completeImageUpload(Long scheduleId, UUID studentId) {
        Staff teacher = getCurrentTeacher();
        ensureTeacherCanUpload(scheduleId, teacher.getId());

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Student not found", HttpStatus.NOT_FOUND));
        AnswerSheet answerSheet = answerSheetRepository.findByExamScheduleIdAndStudentId(scheduleId, student.getId())
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Answer sheet not found", HttpStatus.NOT_FOUND));

        if (!Objects.equals(answerSheet.getUploadedByTeacher().getId(), teacher.getId())) {
            throw new EdusyncException("EVAL-403", "Only the assigned uploader can mark upload complete", HttpStatus.FORBIDDEN);
        }
        long imageCount = answerSheetImageRepository.findByAnswerSheetIdOrderByPageNumberAsc(answerSheet.getId()).size();
        if (imageCount == 0) {
            throw new EdusyncException("EVAL-400", "Cannot complete upload with no pages", HttpStatus.BAD_REQUEST);
        }

        answerSheet.setStatus(AnswerSheetStatus.COMPLETE);
        AnswerSheet saved = answerSheetRepository.save(answerSheet);

        // Update uploader's upload_status to IN_PROGRESS (at least one student sheet completed)
        assignmentRepository.findByExamScheduleIdAndTeacherIdAndRole(
                scheduleId, teacher.getId(), EvaluationAssignmentRole.UPLOADER
        ).ifPresent(uploaderAssignment -> {
            if (uploaderAssignment.getUploadStatus() == UploadStatus.NOT_STARTED) {
                uploaderAssignment.setUploadStatus(UploadStatus.IN_PROGRESS);
                assignmentRepository.save(uploaderAssignment);
            }
        });

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scheduleId", scheduleId);
        metadata.put("studentId", student.getId());
        metadata.put("uploadedPages", imageCount);
        evaluationAuditService.record(EvaluationAuditEventType.ANSWER_SHEET_UPLOAD_COMPLETED, teacher, null, saved, null, metadata);
        return toImageGroupResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AnswerEvaluationStructureResponseDTO getEvaluationStructure(Long answerSheetId) {
        Staff teacher = getCurrentTeacher();
        AnswerSheet answerSheet = getAnswerSheetForEvaluator(answerSheetId, teacher.getId());

        TemplateSnapshot snapshot = Optional.ofNullable(answerSheet.getExamSchedule().getTemplateSnapshot())
                .orElseThrow(() -> new EdusyncException("EVAL-400", "No template snapshot found for schedule", HttpStatus.BAD_REQUEST));

        EvaluationResult result = evaluationResultRepository.findByAnswerSheetId(answerSheetId).orElse(null);
        Map<String, QuestionMark> existingMarkMap = new HashMap<>();
        if (result != null) {
            questionMarkRepository.findByEvaluationResultIdOrderBySectionNameAscQuestionNumberAscOptionLabelAsc(result.getId())
                    .forEach(mark -> existingMarkMap.put(buildQuestionKey(mark.getSectionName(), mark.getQuestionNumber(), mark.getOptionLabel()), mark));
        } else {
            for (SaveQuestionMarkRequestDTO draftMark : evaluationDraftStoreService.getDraft(answerSheetId)) {
                QuestionMark mark = QuestionMark.builder()
                        .sectionName(draftMark.getSectionName())
                        .questionNumber(draftMark.getQuestionNumber())
                        .optionLabel(normalizeOptionLabel(draftMark.getOptionLabel()))
                        .marksObtained(draftMark.getMarksObtained())
                        .annotationType(draftMark.getAnnotationType() == null ? AnnotationType.NONE : draftMark.getAnnotationType())
                        .build();
                existingMarkMap.put(buildQuestionKey(mark.getSectionName(), mark.getQuestionNumber(), mark.getOptionLabel()), mark);
            }
        }

        List<AnswerEvaluationStructureResponseDTO.SectionDTO> sections = new ArrayList<>();
        int totalQuestions = 0;
        BigDecimal totalMax = BigDecimal.ZERO;

        List<TemplateSnapshotSection> orderedSections = snapshot.getSections().stream()
                .sorted(Comparator.comparing(TemplateSnapshotSection::getSectionOrder))
                .collect(Collectors.toList());

        for (TemplateSnapshotSection section : orderedSections) {
            List<AnswerEvaluationStructureResponseDTO.QuestionDTO> questions = new ArrayList<>();
            List<TemplateSnapshotQuestion> snapshotQuestions = getSnapshotQuestions(section);
            for (TemplateSnapshotQuestion snapshotQuestion : snapshotQuestions) {
                BigDecimal max = BigDecimal.valueOf(snapshotQuestion.getMarks());
                String key = buildQuestionKey(section.getName(), snapshotQuestion.getQuestionNo(), "");
                QuestionMark mark = existingMarkMap.get(key);
                List<AnswerEvaluationStructureResponseDTO.OptionDTO> options = null;
                if ((snapshotQuestion.getType() == null ? TemplateQuestionType.NORMAL : snapshotQuestion.getType()) == TemplateQuestionType.INTERNAL_CHOICE) {
                    options = new ArrayList<>();
                    for (String optionLabel : (snapshotQuestion.getOptions() == null ? List.<String>of() : snapshotQuestion.getOptions())) {
                        QuestionMark optionMark = existingMarkMap.get(buildQuestionKey(section.getName(), snapshotQuestion.getQuestionNo(), optionLabel));
                        options.add(AnswerEvaluationStructureResponseDTO.OptionDTO.builder()
                                .label(optionLabel)
                                .maxMarks(max)
                                .marksObtained(optionMark != null ? optionMark.getMarksObtained() : null)
                                .annotationType(optionMark != null ? optionMark.getAnnotationType() : AnnotationType.NONE)
                                .build());
                    }
                }
                questions.add(AnswerEvaluationStructureResponseDTO.QuestionDTO.builder()
                        .questionNumber(snapshotQuestion.getQuestionNo())
                        .maxMarks(max)
                        .type(snapshotQuestion.getType() == null ? TemplateQuestionType.NORMAL : snapshotQuestion.getType())
                        .options(options)
                        .marksObtained(mark != null ? mark.getMarksObtained() : null)
                        .annotationType(mark != null ? mark.getAnnotationType() : AnnotationType.NONE)
                        .build());
                totalQuestions++;
            }
            totalMax = totalMax.add(resolveSectionMaxMarks(section, snapshotQuestions));
            sections.add(AnswerEvaluationStructureResponseDTO.SectionDTO.builder()
                    .sectionName(section.getName())
                    .totalQuestions(resolveTotalQuestions(section))
                    .attemptQuestions(resolveAttemptQuestions(section))
                    .sectionType(resolveSectionType(section))
                    .helperText(resolveSectionType(section) == TemplateSectionType.OPTIONAL
                            ? "Best " + resolveAttemptQuestions(section) + " answers will be considered automatically"
                            : null)
                    .questions(questions)
                    .build());
        }

        return AnswerEvaluationStructureResponseDTO.builder()
                .answerSheetId(answerSheet.getId())
                .totalQuestions(totalQuestions)
                .totalMaxMarks(totalMax.intValue())
                .resultStatus(result != null ? result.getStatus().name() : EvaluationResultStatus.DRAFT.name())
                .sections(sections)
                .build();
    }

    @Override
    @CacheEvict(value = {CacheNames.SCHEDULE_STUDENTS, CacheNames.STUDENT_EVALUATION_RESULTS}, allEntries = true)
    public EvaluationResultResponseDTO saveDraftMarks(Long answerSheetId, SaveEvaluationMarksRequestDTO requestDTO) {
        Object lock = draftSaveLocks.computeIfAbsent(answerSheetId, key -> new Object());
        synchronized (lock) {
            Staff teacher = getCurrentTeacher();
            AnswerSheet answerSheet = getAnswerSheetForEvaluator(answerSheetId, teacher.getId());
            List<SaveQuestionMarkRequestDTO> normalizedMarks = normalizeQuestionMarks(requestDTO.getQuestionMarks());
            evaluationDraftStoreService.saveDraft(answerSheetId, normalizedMarks);

            EvaluationResult result = evaluationResultRepository.findByAnswerSheetId(answerSheetId)
                    .orElseGet(() -> EvaluationResult.builder()
                            .answerSheet(answerSheet)
                            .totalMarks(BigDecimal.ZERO)
                            .status(EvaluationResultStatus.DRAFT)
                            .evaluatedAt(LocalDateTime.now())
                            .build());

            if (result.getStatus() == EvaluationResultStatus.SUBMITTED
                    || result.getStatus() == EvaluationResultStatus.APPROVED
                    || result.getStatus() == EvaluationResultStatus.PUBLISHED) {
                throw new EdusyncException("EVAL-409", "Submitted or published evaluation cannot be modified", HttpStatus.CONFLICT);
            }

            Map<String, BigDecimal> maxByQuestionKey = buildMaxMarksMap(answerSheet.getExamSchedule().getTemplateSnapshot());

            if (result.getId() == null) {
                try {
                    result = evaluationResultRepository.saveAndFlush(result);
                } catch (DataIntegrityViolationException ex) {
                    // Another request inserted the row first; recover and continue.
                    result = evaluationResultRepository.findByAnswerSheetId(answerSheetId)
                            .orElseThrow(() -> ex);
                }
            }

            List<QuestionMark> existingMarks = questionMarkRepository
                    .findByEvaluationResultIdOrderBySectionNameAscQuestionNumberAscOptionLabelAsc(result.getId());
            Map<String, QuestionMark> existingMarkByKey = existingMarks.stream()
                    .collect(Collectors.toMap(
                            mark -> buildQuestionKey(mark.getSectionName(), mark.getQuestionNumber(), mark.getOptionLabel()),
                            mark -> mark,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));

            for (SaveQuestionMarkRequestDTO markRequest : normalizedMarks) {
                String key = buildQuestionKey(markRequest.getSectionName(), markRequest.getQuestionNumber(), markRequest.getOptionLabel());
                BigDecimal maxMarks = maxByQuestionKey.get(key);
                if (maxMarks == null) {
                    throw new EdusyncException("EVAL-400", "Invalid section/question combination: " + key, HttpStatus.BAD_REQUEST);
                }
                BigDecimal value = markRequest.getMarksObtained() == null ? BigDecimal.ZERO : markRequest.getMarksObtained();
                if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(maxMarks) > 0) {
                    throw new EdusyncException("EVAL-400", "marksObtained out of range for " + key, HttpStatus.BAD_REQUEST);
                }

                QuestionMark questionMark = existingMarkByKey.remove(key);
                if (questionMark == null) {
                    String normalizedSection = markRequest.getSectionName().trim();
                    String optionLabel = normalizeOptionLabel(markRequest.getOptionLabel());
                    questionMark = questionMarkRepository
                            .findByEvaluationResultIdAndSectionNameAndQuestionNumberAndOptionLabel(
                                    result.getId(),
                                    normalizedSection,
                                    markRequest.getQuestionNumber(),
                                    optionLabel)
                            .orElse(null);
                    if (questionMark == null) {
                        questionMark = QuestionMark.builder()
                                .evaluationResult(result)
                                .sectionName(normalizedSection)
                                .questionNumber(markRequest.getQuestionNumber())
                                .optionLabel(optionLabel)
                                .build();
                    }
                }
                questionMark.setOptionLabel(normalizeOptionLabel(markRequest.getOptionLabel()));
                questionMark.setMarksObtained(value);
                questionMark.setMaxMarks(maxMarks);
                questionMark.setAnnotationType(markRequest.getAnnotationType() == null ? AnnotationType.NONE : markRequest.getAnnotationType());
                try {
                    questionMarkRepository.saveAndFlush(questionMark);
                } catch (DataIntegrityViolationException ex) {
                    // Last-chance recovery for race on unique key.
                    QuestionMark recovered = questionMarkRepository
                            .findByEvaluationResultIdAndSectionNameAndQuestionNumberAndOptionLabel(
                                    result.getId(),
                                    markRequest.getSectionName().trim(),
                                    markRequest.getQuestionNumber(),
                                    normalizeOptionLabel(markRequest.getOptionLabel()))
                            .orElseThrow(() -> ex);
                    recovered.setMarksObtained(value);
                    recovered.setMaxMarks(maxMarks);
                    recovered.setAnnotationType(markRequest.getAnnotationType() == null ? AnnotationType.NONE : markRequest.getAnnotationType());
                    questionMarkRepository.save(recovered);
                }
            }

            if (!existingMarkByKey.isEmpty()) {
                questionMarkRepository.deleteAllInBatch(existingMarkByKey.values());
            }

            EvaluationScoringCalculator.ScoreComputationResult score = EvaluationScoringCalculator.compute(
                    answerSheet.getExamSchedule().getTemplateSnapshot(),
                    questionMarkRepository.findByEvaluationResultIdOrderBySectionNameAscQuestionNumberAscOptionLabelAsc(result.getId())
            );
            result.setTotalMarks(score.totalMarks());
            result.setSectionTotals(score.sectionTotals());
            result.setSelectedQuestions(score.selectedQuestions());
            result.setStatus(EvaluationResultStatus.DRAFT);
            result.setEvaluatedAt(LocalDateTime.now());
            result.setApprovedAt(null);
            result.setPublishedAt(null);
            result.setApprovedBy(null);
            EvaluationResult saved = evaluationResultRepository.save(result);

            answerSheet.setStatus(AnswerSheetStatus.DRAFT);
            answerSheetRepository.save(answerSheet);
            markAssignmentInProgress(answerSheet.getExamSchedule().getId(), teacher.getId());
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("totalMarks", saved.getTotalMarks());
            metadata.put("questionCount", normalizedMarks.size());
            metadata.put("status", saved.getStatus().name());
            evaluationAuditService.record(EvaluationAuditEventType.DRAFT_MARKS_SAVED, teacher, null, answerSheet, saved, metadata);
            log.info("Draft marks saved: answerSheetId={}, totalMarks={}", answerSheetId, saved.getTotalMarks());

            return toEvaluationResultResponse(saved);
        }
    }

    @Override
    @CacheEvict(value = {CacheNames.SCHEDULE_STUDENTS, CacheNames.STUDENT_EVALUATION_RESULTS}, allEntries = true)
    public EvaluationResultResponseDTO submitMarks(Long answerSheetId) {
        Staff teacher = getCurrentTeacher();
        AnswerSheet answerSheet = getAnswerSheetForEvaluator(answerSheetId, teacher.getId());

        EvaluationResult result = evaluationResultRepository.findByAnswerSheetId(answerSheetId)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "No draft marks found for answer sheet", HttpStatus.NOT_FOUND));

        if (result.getStatus() == EvaluationResultStatus.SUBMITTED) {
            return toEvaluationResultResponse(result);
        }
        if (result.getStatus() != EvaluationResultStatus.DRAFT
                && result.getStatus() != EvaluationResultStatus.REJECTED) {
            throw new EdusyncException("EVAL-409", "Only draft or rejected results can be submitted", HttpStatus.CONFLICT);
        }

        result.setStatus(EvaluationResultStatus.SUBMITTED);
        result.setSubmittedAt(LocalDateTime.now());
        result.setApprovedAt(null);
        result.setPublishedAt(null);
        result.setApprovedBy(null);
        result.setEvaluatedAt(LocalDateTime.now());
        EvaluationResult saved = evaluationResultRepository.save(result);

        answerSheet.setStatus(AnswerSheetStatus.CHECKING);
        answerSheetRepository.save(answerSheet);
        markAssignmentCompleted(answerSheet.getExamSchedule().getId(), teacher.getId());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("totalMarks", saved.getTotalMarks());
        metadata.put("status", saved.getStatus().name());
        evaluationAuditService.record(EvaluationAuditEventType.MARKS_SUBMITTED, teacher, null, answerSheet, saved, metadata);
        log.info("Evaluation submitted: answerSheetId={}, totalMarks={}", answerSheetId, saved.getTotalMarks());

        return toEvaluationResultResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminResultReviewResponseDTO> getResultsForAdmin(EvaluationResultStatus status) {
        requireAdmin();
        List<EvaluationResult> results = status == null
                ? evaluationResultRepository.findAllWithContext()
                : evaluationResultRepository.findAllByStatusWithContext(status);
        return results.stream().map(this::toAdminResultResponse).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = {CacheNames.SCHEDULE_STUDENTS, CacheNames.STUDENT_EVALUATION_RESULTS}, allEntries = true)
    public EvaluationResultResponseDTO approveResult(Long resultId) {
        requireAdmin();
        EvaluationResult result = getResultForAdmin(resultId);
        if (result.getStatus() != EvaluationResultStatus.SUBMITTED) {
            throw new EdusyncException("EVAL-409", "Only submitted results can be approved", HttpStatus.CONFLICT);
        }

        result.setStatus(EvaluationResultStatus.APPROVED);
        result.setApprovedAt(LocalDateTime.now());
        result.setApprovedBy(authUtil.getCurrentUser());
        EvaluationResult saved = evaluationResultRepository.save(result);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("resultId", saved.getId());
        metadata.put("status", saved.getStatus().name());
        metadata.put("approvedBy", saved.getApprovedBy() != null ? saved.getApprovedBy().getUsername() : null);
        evaluationAuditService.record(EvaluationAuditEventType.MARKS_APPROVED, null, null, saved.getAnswerSheet(), saved, metadata);
        return toEvaluationResultResponse(saved);
    }

    @Override
    @CacheEvict(value = {CacheNames.SCHEDULE_STUDENTS, CacheNames.STUDENT_EVALUATION_RESULTS}, allEntries = true)
    public EvaluationResultResponseDTO rejectResult(Long resultId) {
        requireAdmin();
        EvaluationResult result = getResultForAdmin(resultId);
        if (result.getStatus() != EvaluationResultStatus.SUBMITTED) {
            throw new EdusyncException("EVAL-409", "Only submitted results can be rejected", HttpStatus.CONFLICT);
        }

        result.setStatus(EvaluationResultStatus.REJECTED);
        result.setApprovedAt(null);
        result.setPublishedAt(null);
        result.setApprovedBy(authUtil.getCurrentUser());
        EvaluationResult saved = evaluationResultRepository.save(result);

        AnswerSheet answerSheet = saved.getAnswerSheet();
        answerSheet.setStatus(AnswerSheetStatus.DRAFT);
        answerSheetRepository.save(answerSheet);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("resultId", saved.getId());
        metadata.put("status", saved.getStatus().name());
        evaluationAuditService.record(EvaluationAuditEventType.MARKS_REJECTED, null, null, answerSheet, saved, metadata);
        return toEvaluationResultResponse(saved);
    }

    @Override
    @CacheEvict(value = {CacheNames.SCHEDULE_STUDENTS, CacheNames.STUDENT_EVALUATION_RESULTS}, allEntries = true)
    public EvaluationResultResponseDTO publishResult(Long resultId) {
        requireAdmin();
        EvaluationResult result = getResultForAdmin(resultId);
        if (result.getStatus() == EvaluationResultStatus.PUBLISHED) {
            return toEvaluationResultResponse(result);
        }
        if (result.getStatus() != EvaluationResultStatus.APPROVED) {
            throw new EdusyncException("EVAL-409", "Only approved results can be published", HttpStatus.CONFLICT);
        }

        result.setStatus(EvaluationResultStatus.PUBLISHED);
        result.setPublishedAt(LocalDateTime.now());
        if (result.getApprovedBy() == null) {
            result.setApprovedBy(authUtil.getCurrentUser());
        }
        EvaluationResult saved = evaluationResultRepository.save(result);

        AnswerSheet answerSheet = saved.getAnswerSheet();
        answerSheet.setStatus(AnswerSheetStatus.FINAL);
        answerSheetRepository.save(answerSheet);
        evaluationDraftStoreService.deleteDraft(answerSheet.getId());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("resultId", saved.getId());
        metadata.put("status", saved.getStatus().name());
        metadata.put("publishedAt", saved.getPublishedAt() != null ? saved.getPublishedAt().toString() : null);
        evaluationAuditService.record(EvaluationAuditEventType.MARKS_PUBLISHED, null, null, answerSheet, saved, metadata);
        return toEvaluationResultResponse(saved);
    }

    @Override
    @CacheEvict(value = {CacheNames.SCHEDULE_STUDENTS, CacheNames.STUDENT_EVALUATION_RESULTS}, allEntries = true)
    public int publishResultsBulk(List<Long> resultIds) {
        requireAdmin();
        if (resultIds == null || resultIds.isEmpty()) return 0;
        
        List<EvaluationResult> results = evaluationResultRepository.findAllByIdInAndStatusWithContext(resultIds, EvaluationResultStatus.APPROVED);
        if (results.isEmpty()) return 0;

        LocalDateTime now = LocalDateTime.now();
        User currentUser = authUtil.getCurrentUser();
        List<AnswerSheet> updatedAnswerSheets = new ArrayList<>();

        for (EvaluationResult result : results) {
            result.setStatus(EvaluationResultStatus.PUBLISHED);
            result.setPublishedAt(now);
            if (result.getApprovedBy() == null) {
                result.setApprovedBy(currentUser);
            }
            AnswerSheet answerSheet = result.getAnswerSheet();
            answerSheet.setStatus(AnswerSheetStatus.FINAL);
            updatedAnswerSheets.add(answerSheet);
            evaluationDraftStoreService.deleteDraft(answerSheet.getId());
        }

        List<EvaluationResult> savedResults = evaluationResultRepository.saveAll(results);
        answerSheetRepository.saveAll(updatedAnswerSheets);

        for (EvaluationResult saved : savedResults) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("resultId", saved.getId());
            metadata.put("status", saved.getStatus().name());
            metadata.put("publishedAt", saved.getPublishedAt() != null ? saved.getPublishedAt().toString() : null);
            metadata.put("bulk", true);
            evaluationAuditService.record(EvaluationAuditEventType.MARKS_PUBLISHED, null, null, saved.getAnswerSheet(), saved, metadata);
        }
        return savedResults.size();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResultSummaryResponseDTO getClassResultSummary(UUID classId, UUID examId) {
        requireAdmin();
        
        List<ExamSchedule> allSchedules = examScheduleRepository.findByExamUuid(examId);
        List<ExamSchedule> classSchedules = allSchedules.stream()
            .filter(es -> es.getAcademicClass().getUuid().equals(classId))
            .collect(Collectors.toList());
            
        if (classSchedules.isEmpty()) {
            throw new EdusyncException("EVAL-404", "No schedules found for class and exam", HttpStatus.NOT_FOUND);
        }
        
        Long internalExamId = classSchedules.get(0).getExam().getId();
        
        List<Object[]> counts = examScheduleRepository.countActiveStudentsPerSchedule(internalExamId);
        long totalStudents = 0;
        List<Long> classScheduleIds = classSchedules.stream().map(ExamSchedule::getId).collect(Collectors.toList());
        for (Object[] row : counts) {
            Long schedId = ((Number) row[0]).longValue();
            if (classScheduleIds.contains(schedId)) {
                totalStudents += ((Number) row[1]).longValue();
            }
        }
        
        long absentStudents = studentExamStatusRepository.countAbsentStudentsByClassAndExam(classId, examId);
        
        List<EvaluationResult> results = evaluationResultRepository.findAllWithContext().stream()
             .filter(r -> classScheduleIds.contains(r.getAnswerSheet().getExamSchedule().getId()))
             .collect(Collectors.toList());
             
        long evaluatedStudents = results.stream()
            .filter(r -> r.getStatus() == EvaluationResultStatus.SUBMITTED || 
                         r.getStatus() == EvaluationResultStatus.APPROVED || 
                         r.getStatus() == EvaluationResultStatus.PUBLISHED)
            .count();
            
        long pendingStudents = totalStudents - absentStudents - evaluatedStudents;
        
        String status = "INCOMPLETE";
        if (pendingStudents <= 0) {
            long publishedCount = results.stream().filter(r -> r.getStatus() == EvaluationResultStatus.PUBLISHED).count();
            long approvedCount = results.stream().filter(r -> r.getStatus() == EvaluationResultStatus.APPROVED).count();
            if (publishedCount == evaluatedStudents && evaluatedStudents > 0) {
                status = "PUBLISHED";
            } else if (approvedCount + publishedCount == evaluatedStudents && evaluatedStudents > 0) {
                status = "APPROVED";
            } else {
                status = "READY_FOR_APPROVAL";
            }
        }
        
        return ClassResultSummaryResponseDTO.builder()
                .classId(classId)
                .className(classSchedules.get(0).getAcademicClass().getName())
                .examId(examId)
                .examName(classSchedules.get(0).getExam().getName())
                .totalStudents(totalStudents)
                .evaluatedStudents(evaluatedStudents)
                .absentStudents(absentStudents)
                .pendingStudents(Math.max(0, pendingStudents))
                .status(status)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {CacheNames.SCHEDULE_STUDENTS, CacheNames.STUDENT_EVALUATION_RESULTS}, allEntries = true)
    public int approveClassResults(UUID classId, UUID examId) {
        requireAdmin();
        ClassResultSummaryResponseDTO summary = getClassResultSummary(classId, examId);
        if (summary.getPendingStudents() > 0) {
            throw new EdusyncException("EVAL-409", "Cannot approve class yet. Still " + summary.getPendingStudents() + " students pending evaluation or absent marking.", HttpStatus.CONFLICT);
        }
        
        List<ExamSchedule> allSchedules = examScheduleRepository.findByExamUuid(examId);
        List<Long> classScheduleIds = allSchedules.stream()
            .filter(es -> es.getAcademicClass().getUuid().equals(classId))
            .map(ExamSchedule::getId)
            .collect(Collectors.toList());
            
        List<EvaluationResult> results = evaluationResultRepository.findAllWithContext().stream()
             .filter(r -> classScheduleIds.contains(r.getAnswerSheet().getExamSchedule().getId()))
             .filter(r -> r.getStatus() == EvaluationResultStatus.SUBMITTED)
             .collect(Collectors.toList());
             
        User currentUser = authUtil.getCurrentUser();
        for (EvaluationResult result : results) {
            result.setStatus(EvaluationResultStatus.APPROVED);
            result.setApprovedAt(LocalDateTime.now());
            result.setApprovedBy(currentUser);
        }
        evaluationResultRepository.saveAll(results);
        return results.size();
    }

    @Override
    @Transactional
    @CacheEvict(value = {CacheNames.SCHEDULE_STUDENTS, CacheNames.STUDENT_EVALUATION_RESULTS}, allEntries = true)
    public int publishClassResults(UUID classId, UUID examId) {
        requireAdmin();
        List<ExamSchedule> allSchedules = examScheduleRepository.findByExamUuid(examId);
        List<Long> classScheduleIds = allSchedules.stream()
            .filter(es -> es.getAcademicClass().getUuid().equals(classId))
            .map(ExamSchedule::getId)
            .collect(Collectors.toList());
            
        List<EvaluationResult> results = evaluationResultRepository.findAllWithContext().stream()
             .filter(r -> classScheduleIds.contains(r.getAnswerSheet().getExamSchedule().getId()))
             .filter(r -> r.getStatus() == EvaluationResultStatus.APPROVED)
             .collect(Collectors.toList());
             
        if (results.isEmpty()) return 0;
        
        List<Long> resultIds = results.stream().map(EvaluationResult::getId).collect(Collectors.toList());
        return publishResultsBulk(resultIds);
    }

    @Override
    @Transactional
    public void markStudentAbsent(Long scheduleId, Long studentId, boolean isAbsent) {
        ExamSchedule schedule = getSchedule(scheduleId);
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new EdusyncException("EVAL-404", "Student not found", HttpStatus.NOT_FOUND));
            
        StudentExamStatus status = studentExamStatusRepository.findByStudentIdAndExamScheduleId(studentId, scheduleId)
            .orElseGet(() -> StudentExamStatus.builder()
                .student(student)
                .examSchedule(schedule)
                .build());
                
        status.setIsAbsent(isAbsent);
        studentExamStatusRepository.save(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResultResponseDTO> getStudentPublishedResults() {
        Student student = getCurrentStudent();
        return evaluationResultRepository
                .findByStudentIdAndStatusWithContext(student.getId(), EvaluationResultStatus.PUBLISHED)
                .stream()
                .map(this::toStudentResultResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.STUDENT_EVALUATION_RESULTS,
            key = "#resultId + ':' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public StudentResultDetailResponseDTO getStudentPublishedResult(Long resultId) {
        return toStudentResultDetailResponse(getPublishedResultForCurrentStudent(resultId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStudentResultPdf(Long resultId) {
        EvaluationResult result = getPublishedResultForCurrentStudent(resultId);
        StudentResultDetailResponseDTO detail = toStudentResultDetailResponse(result);

        Student student = result.getAnswerSheet().getStudent();
        String studentName = (student.getUserProfile().getFirstName() + " " + student.getUserProfile().getLastName()).trim();

        Map<String, Object> data = new HashMap<>();
        data.put("studentName", studentName);
        data.put("enrollmentNumber", student.getEnrollmentNumber());
        data.put("examName", detail.getExamName());
        data.put("examDate", detail.getExamDate());
        data.put("subjectMarks", detail.getSubjectMarks());
        data.put("totalMarks", detail.getTotalMarks());
        data.put("maxMarks", detail.getMaxMarks());
        data.put("publishedAt", detail.getPublishedAt());
        return pdfGenerationService.generatePdfFromHtml("em/result-sheet", data);
    }

    @Override
    public AnnotationResponseDTO createAnnotation(Long answerSheetId, AnnotationRequestDTO requestDTO) {
        Staff teacher = getCurrentTeacher();
        AnswerSheet answerSheet = getAnswerSheetForEvaluator(answerSheetId, teacher.getId());
        validateAnnotation(requestDTO);

        AnswerSheetAnnotation annotation = AnswerSheetAnnotation.builder()
                .answerSheet(answerSheet)
                .pageNumber(requestDTO.getPageNumber())
                .x(requestDTO.getX())
                .y(requestDTO.getY())
                .type(requestDTO.getType())
                .metadata(sanitizeMetadata(requestDTO.getMetadata()))
                .build();

        AnswerSheetAnnotation saved = annotationRepository.save(annotation);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("annotationId", saved.getId());
        metadata.put("pageNumber", saved.getPageNumber());
        metadata.put("type", saved.getType().name());
        evaluationAuditService.record(EvaluationAuditEventType.ANNOTATION_CREATED, teacher, null, answerSheet, null, metadata);
        return toAnnotationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationResponseDTO> getAnnotations(Long answerSheetId) {
        Staff teacher = getCurrentTeacher();
        getAnswerSheetForTeacher(answerSheetId, teacher.getId());
        return annotationRepository.findByAnswerSheetIdOrderByPageNumberAscIdAsc(answerSheetId).stream()
                .map(this::toAnnotationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String generateSignedFileUrl(Long answerSheetId) {
        Staff teacher = getCurrentTeacher();
        getAnswerSheetForTeacher(answerSheetId, teacher.getId());

        long expires = System.currentTimeMillis() + (10 * 60 * 1000);
        String signature = sign(answerSheetId, expires);
        return apiUrl + "/teacher/answer-sheets/" + answerSheetId + "/file?token=" + signature + "&expires=" + expires;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadSignedAnswerSheet(Long answerSheetId, String token, long expires) {
        if (System.currentTimeMillis() > expires) {
            throw new EdusyncException("EVAL-401", "Signed URL expired", HttpStatus.UNAUTHORIZED);
        }
        String expected = sign(answerSheetId, expires);
        if (!constantTimeEquals(expected, token)) {
            throw new EdusyncException("EVAL-401", "Invalid signed URL token", HttpStatus.UNAUTHORIZED);
        }

        AnswerSheet answerSheet = answerSheetRepository.findById(answerSheetId)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Answer sheet not found", HttpStatus.NOT_FOUND));

        try {
            Resource resource;
            if (answerSheet.getFileUrl() != null && answerSheet.getFileUrl().startsWith("http")) {
                // Cloudinary URL
                resource = new UrlResource(answerSheet.getFileUrl());
            } else {
                // Legacy local storage
                Path filePath = ensureStorageRoot().resolve(answerSheet.getFileUrl()).normalize();
                resource = new UrlResource(filePath.toUri());
            }
            
            if (!resource.exists() || !resource.isReadable()) {
                throw new EdusyncException("EVAL-404", "Answer sheet file not found", HttpStatus.NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new EdusyncException("EVAL-500", "Failed to read answer sheet file", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnswerSheetUploadResponseDTO> getAnswerSheetsForAssignedSchedule(Long scheduleId, int page, int size) {
        Staff teacher = getCurrentTeacher();
        ensureTeacherAssigned(scheduleId, teacher.getId());

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(Math.min(size, 100), 1);
        Page<AnswerSheet> sheets = answerSheetRepository.findByScheduleId(scheduleId, PageRequest.of(safePage, safeSize));

        List<AnswerSheetUploadResponseDTO> content = sheets.getContent().stream()
                .map(sheet -> AnswerSheetUploadResponseDTO.builder()
                        .answerSheetId(sheet.getId())
                        .fileUrl(apiUrl + "/teacher/answer-sheets/" + sheet.getId() + "/signed-url")
                        .status(sheet.getStatus())
                        .createdAt(sheet.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(content, sheets.getPageable(), sheets.getTotalElements());
    }

    private void validatePdfUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EdusyncException("EVAL-400", "PDF file is required", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > PDF_MAX_SIZE_BYTES) {
            throw new EdusyncException("EVAL-400", "File exceeds max size of 10MB", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equalsIgnoreCase("application/pdf") || contentType.equalsIgnoreCase("application/x-pdf"))) {
            throw new EdusyncException("EVAL-400", "Only PDF uploads are allowed", HttpStatus.BAD_REQUEST);
        }

        try {
            byte[] header = file.getBytes();
            if (header.length < 5) {
                throw new EdusyncException("EVAL-400", "Invalid PDF file", HttpStatus.BAD_REQUEST);
            }
            String magic = new String(header, 0, 5, StandardCharsets.US_ASCII);
            if (!PDF_MAGIC.equals(magic)) {
                throw new EdusyncException("EVAL-400", "Invalid PDF signature", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new EdusyncException("EVAL-400", "Unable to validate file contents", HttpStatus.BAD_REQUEST, e);
        }

        String originalName = file.getOriginalFilename();
        if (StringUtils.hasText(originalName) && originalName.toLowerCase(Locale.ROOT).endsWith(".php")) {
            throw new EdusyncException("EVAL-400", "Invalid file name", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateImageUpload(MultipartFile file, Integer pageNumber) {
        if (file == null || file.isEmpty()) {
            throw new EdusyncException("EVAL-400", "Image file is required", HttpStatus.BAD_REQUEST);
        }
        if (pageNumber == null || pageNumber < 1) {
            throw new EdusyncException("EVAL-400", "pageNumber must be >= 1", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new EdusyncException("EVAL-400", "Image exceeds configured max size", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(normalized)) {
            throw new EdusyncException("EVAL-400", "Only jpeg/png image uploads are allowed", HttpStatus.BAD_REQUEST);
        }
    }

    private String uploadImageToCloudinary(MultipartFile file, Long answerSheetId, Integer pageNumber) {
        try {
            MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
            String folder = cfg.getFolder() != null ? cfg.getFolder() : "answer-sheets";
            String publicId = folder + "/images/" + answerSheetId + "/page-" + pageNumber + "-" + UUID.randomUUID();
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "overwrite", true
            ));
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            log.error("Cloudinary image upload failed for answerSheetId={}, page={}", answerSheetId, pageNumber, e);
            throw new EdusyncException("EVAL-500", "Failed to upload answer sheet image", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Student getValidatedStudentForSchedule(UUID studentUuid, ExamSchedule schedule) {
        Student student = studentRepository.findByUuid(studentUuid)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Student not found", HttpStatus.NOT_FOUND));

        if (schedule.getSection() != null && !Objects.equals(student.getSection().getId(), schedule.getSection().getId())) {
            throw new EdusyncException("EVAL-400", "Student does not belong to assigned section", HttpStatus.BAD_REQUEST);
        }
        if (schedule.getSection() == null && !Objects.equals(student.getSection().getAcademicClass().getId(), schedule.getAcademicClass().getId())) {
            throw new EdusyncException("EVAL-400", "Student does not belong to assigned class", HttpStatus.BAD_REQUEST);
        }
        return student;
    }

    private List<Integer> resolvePageNumbers(List<Integer> pageNumbers, int fileCount) {
        if (pageNumbers == null || pageNumbers.isEmpty()) {
            List<Integer> generated = new ArrayList<>();
            for (int i = 1; i <= fileCount; i++) {
                generated.add(i);
            }
            return generated;
        }
        if (pageNumbers.size() != fileCount) {
            throw new EdusyncException("EVAL-400", "pageNumbers count must match files count", HttpStatus.BAD_REQUEST);
        }

        Set<Integer> uniquePages = new HashSet<>();
        for (Integer pageNumber : pageNumbers) {
            if (pageNumber == null || pageNumber < 1) {
                throw new EdusyncException("EVAL-400", "pageNumbers must be >= 1", HttpStatus.BAD_REQUEST);
            }
            if (!uniquePages.add(pageNumber)) {
                throw new EdusyncException("EVAL-400", "Duplicate pageNumber in upload request", HttpStatus.BAD_REQUEST);
            }
        }
        return pageNumbers;
    }

    private void enforceUploadRateLimit(Long teacherId) {
        long now = System.currentTimeMillis();
        Deque<Long> window = uploadWindow.computeIfAbsent(teacherId, t -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > 60_000) {
                window.pollFirst();
            }
            if (window.size() >= maxUploadsPerMinute) {
                throw new EdusyncException("EVAL-429", "Upload rate limit exceeded. Try again shortly.", HttpStatus.TOO_MANY_REQUESTS);
            }
            window.addLast(now);
        }
    }

    private Path ensureStorageRoot() {
        Path root = Paths.get(privateStorageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new EdusyncException("EVAL-500", "Unable to initialize private storage", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        return root;
    }

    private Staff getCurrentTeacher() {
        Long userId = authUtil.getCurrentUserId();
        return staffRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new EdusyncException("EVAL-403", "Current user is not mapped to teacher/staff", HttpStatus.FORBIDDEN));
    }

    private Student getCurrentStudent() {
        Long userId = authUtil.getCurrentUserId();
        return studentRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new EdusyncException("EVAL-403", "Current user is not mapped to student", HttpStatus.FORBIDDEN));
    }

    private void ensureTeacherAssigned(Long scheduleId, Long teacherId) {
        if (!assignmentRepository.existsByExamScheduleIdAndTeacherId(scheduleId, teacherId)) {
            throw new EdusyncException("EVAL-403", "Teacher is not assigned to this schedule", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Upload access control:
     * - If a dedicated UPLOADER exists → only UPLOADER can upload
     * - Otherwise → EVALUATOR can upload
     * - Uploads are locked once any evaluator has started evaluating
     */
    private void ensureTeacherCanUpload(Long scheduleId, Long teacherId) {
        // Check if uploads are locked (evaluator has started)
        if (areUploadsLocked(scheduleId)) {
            throw new EdusyncException("EVAL-403",
                    "Uploads are locked — evaluation has started", HttpStatus.FORBIDDEN);
        }

        boolean hasDedicatedUploader = assignmentRepository.existsByExamScheduleIdAndRole(
                scheduleId, EvaluationAssignmentRole.UPLOADER);

        if (hasDedicatedUploader) {
            if (!assignmentRepository.existsByExamScheduleIdAndTeacherIdAndRole(
                    scheduleId, teacherId, EvaluationAssignmentRole.UPLOADER)) {
                throw new EdusyncException("EVAL-403",
                        "A dedicated uploader is assigned. Only the uploader can upload.", HttpStatus.FORBIDDEN);
            }
        } else {
            // No dedicated uploader → evaluator can upload
            if (!assignmentRepository.existsByExamScheduleIdAndTeacherIdAndRole(
                    scheduleId, teacherId, EvaluationAssignmentRole.EVALUATOR)) {
                throw new EdusyncException("EVAL-403",
                        "Teacher is not assigned to this schedule", HttpStatus.FORBIDDEN);
            }
        }
    }

    /**
     * Evaluation access control:
     * - Only EVALUATOR role
     * - All uploaders must have completed (if any dedicated uploaders exist)
     */
    private void ensureTeacherCanEvaluate(Long scheduleId, Long teacherId) {
        if (!assignmentRepository.existsByExamScheduleIdAndTeacherIdAndRole(
                scheduleId, teacherId, EvaluationAssignmentRole.EVALUATOR)) {
            throw new EdusyncException("EVAL-403",
                    "Only evaluators can access evaluation features", HttpStatus.FORBIDDEN);
        }
        ensureUploadsCompleted(scheduleId);
    }

    /**
     * If there are dedicated uploaders, all must have uploadStatus = COMPLETED.
     */
    private void ensureUploadsCompleted(Long scheduleId) {
        boolean hasDedicatedUploader = assignmentRepository.existsByExamScheduleIdAndRole(
                scheduleId, EvaluationAssignmentRole.UPLOADER);
        if (hasDedicatedUploader) {
            // Check if any uploader has NOT completed
            boolean anyIncomplete = assignmentRepository.existsByExamScheduleIdAndRoleAndUploadStatusNot(
                    scheduleId, EvaluationAssignmentRole.UPLOADER, UploadStatus.COMPLETED);
            if (anyIncomplete) {
                throw new EdusyncException("EVAL-403",
                        "Cannot start evaluation — not all uploaders have completed", HttpStatus.FORBIDDEN);
            }
        }
    }

    /**
     * Uploads are locked once an evaluator has started (status != ASSIGNED).
     */
    private boolean areUploadsLocked(Long scheduleId) {
        return assignmentRepository.existsByExamScheduleIdAndRoleAndStatusNot(
                scheduleId, EvaluationAssignmentRole.EVALUATOR, EvaluationAssignmentStatus.ASSIGNED);
    }

    private AnswerSheet getAnswerSheetForTeacher(Long answerSheetId, Long teacherId) {
        AnswerSheet answerSheet = answerSheetRepository.findByIdWithScheduleAndStudent(answerSheetId)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Answer sheet not found", HttpStatus.NOT_FOUND));
        ensureTeacherAssigned(answerSheet.getExamSchedule().getId(), teacherId);
        return answerSheet;
    }

    private AnswerSheet getAnswerSheetForEvaluator(Long answerSheetId, Long teacherId) {
        AnswerSheet answerSheet = answerSheetRepository.findByIdWithScheduleAndStudent(answerSheetId)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Answer sheet not found", HttpStatus.NOT_FOUND));
        ensureTeacherCanEvaluate(answerSheet.getExamSchedule().getId(), teacherId);
        return answerSheet;
    }

    private ExamSchedule getSchedule(Long scheduleId) {
        return examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND));
    }

    private void requireAdmin() {
        User currentUser = authUtil.getCurrentUser();
        Set<String> authorities = currentUser.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
        if (!(authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_SUPER_ADMIN") || authorities.contains("ROLE_SCHOOL_ADMIN"))) {
            throw new EdusyncException("EVAL-403", "Admin privileges required", HttpStatus.FORBIDDEN);
        }
    }

    private EvaluationAssignmentResponseDTO toAssignmentResponse(EvaluationAssignment assignment) {
        String teacherName = assignment.getTeacher().getUserProfile() != null
                ? (assignment.getTeacher().getUserProfile().getFirstName() + " " + assignment.getTeacher().getUserProfile().getLastName()).trim()
                : assignment.getTeacher().getEmployeeId();
        return EvaluationAssignmentResponseDTO.builder()
                .assignmentId(assignment.getId())
                .examScheduleId(assignment.getExamSchedule().getId())
                .examUuid(assignment.getExamSchedule().getExam().getUuid())
                .examName(assignment.getExamSchedule().getExam().getName())
                .subjectName(assignment.getExamSchedule().getSubject().getName())
                .examDate(assignment.getExamSchedule().getExamDate())
                .teacherId(assignment.getTeacher().getUuid())
                .teacherName(teacherName)
                .role(assignment.getRole().name())
                .status(assignment.getStatus())
                .uploadStatus(assignment.getUploadStatus() != null ? assignment.getUploadStatus().name() : null)
                .assignedAt(assignment.getAssignedAt())
                .dueDate(assignment.getDueDate())
                .build();
    }

    private TeacherEvaluationStudentResponseDTO toTeacherEvaluationStudentResponse(Student student, AnswerSheet sheet) {
        String fullName = student.getUserProfile().getFirstName() + " " + student.getUserProfile().getLastName();
        return TeacherEvaluationStudentResponseDTO.builder()
                .studentId(student.getUuid())
                .studentName(fullName.trim())
                .enrollmentNumber(student.getEnrollmentNumber())
                .answerSheetId(sheet != null ? sheet.getId() : null)
                .answerSheetStatus(sheet != null ? sheet.getStatus() : null)
                .build();
    }

    private AnswerSheetImageGroupResponseDTO toImageGroupResponse(AnswerSheet answerSheet) {
        List<AnswerSheetImagePageResponseDTO> pages = answerSheetImageRepository
                .findByAnswerSheetIdOrderByPageNumberAsc(answerSheet.getId())
                .stream()
                .map(image -> AnswerSheetImagePageResponseDTO.builder()
                        .pageNumber(image.getPageNumber())
                        .imageUrl(image.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        return AnswerSheetImageGroupResponseDTO.builder()
                .answerSheetId(answerSheet.getId())
                .studentId(answerSheet.getStudent().getUuid())
                .examScheduleId(answerSheet.getExamSchedule().getId())
                .status(answerSheet.getStatus())
                .updatedAt(answerSheet.getUpdatedAt())
                .pages(pages)
                .build();
    }

    private EvaluationResultResponseDTO toEvaluationResultResponse(EvaluationResult result) {
        return EvaluationResultResponseDTO.builder()
                .resultId(result.getId())
                .answerSheetId(result.getAnswerSheet().getId())
                .totalMarks(result.getTotalMarks())
                .status(result.getStatus().name())
                .evaluatedAt(result.getEvaluatedAt())
                .submittedAt(result.getSubmittedAt())
                .approvedAt(result.getApprovedAt())
                .publishedAt(result.getPublishedAt())
                .approvedBy(result.getApprovedBy() != null ? result.getApprovedBy().getUsername() : null)
                .sectionTotals(result.getSectionTotals())
                .selectedQuestions(result.getSelectedQuestions())
                .build();
    }

    private AdminResultReviewResponseDTO toAdminResultResponse(EvaluationResult result) {
        AnswerSheet sheet = result.getAnswerSheet();
        Student student = sheet.getStudent();
        ExamSchedule schedule = sheet.getExamSchedule();
        String studentName = (student.getUserProfile().getFirstName() + " " + student.getUserProfile().getLastName()).trim();

        return AdminResultReviewResponseDTO.builder()
                .resultId(result.getId())
                .answerSheetId(sheet.getId())
                .scheduleId(schedule.getId())
                .studentId(student.getUuid())
                .studentName(studentName)
                .enrollmentNumber(student.getEnrollmentNumber())
                .examId(schedule.getExam().getUuid())
                .examName(schedule.getExam().getName())
                .classId(schedule.getAcademicClass().getUuid())
                .className(schedule.getAcademicClass().getName())
                .subjectName(schedule.getSubject().getName())
                .totalMarks(result.getTotalMarks())
                .status(result.getStatus().name())
                .submittedAt(result.getSubmittedAt())
                .approvedAt(result.getApprovedAt())
                .publishedAt(result.getPublishedAt())
                .approvedBy(result.getApprovedBy() != null ? result.getApprovedBy().getUsername() : null)
                .build();
    }

    private StudentResultResponseDTO toStudentResultResponse(EvaluationResult result) {
        ExamSchedule schedule = result.getAnswerSheet().getExamSchedule();
        return StudentResultResponseDTO.builder()
                .resultId(result.getId())
                .scheduleId(schedule.getId())
                .examName(schedule.getExam().getName())
                .subjectName(schedule.getSubject().getName())
                .marksObtained(result.getTotalMarks())
                .maxMarks(schedule.getMaxMarks())
                .publishedAt(result.getPublishedAt())
                .build();
    }

    private StudentResultDetailResponseDTO toStudentResultDetailResponse(EvaluationResult result) {
        ExamSchedule schedule = result.getAnswerSheet().getExamSchedule();
        StudentResultDetailResponseDTO.SubjectMarkDTO subjectMark = StudentResultDetailResponseDTO.SubjectMarkDTO.builder()
                .subjectName(schedule.getSubject().getName())
                .marksObtained(result.getTotalMarks())
                .maxMarks(schedule.getMaxMarks())
                .build();

        return StudentResultDetailResponseDTO.builder()
                .resultId(result.getId())
                .scheduleId(schedule.getId())
                .examName(schedule.getExam().getName())
                .subjectName(schedule.getSubject().getName())
                .examDate(schedule.getExamDate())
                .totalMarks(result.getTotalMarks())
                .maxMarks(schedule.getMaxMarks())
                .publishedAt(result.getPublishedAt())
                .subjectMarks(List.of(subjectMark))
                .build();
    }

    private EvaluationResult getResultForAdmin(Long resultId) {
        return evaluationResultRepository.findByIdWithContext(resultId)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Evaluation result not found", HttpStatus.NOT_FOUND));
    }

    private EvaluationResult getPublishedResultForCurrentStudent(Long resultId) {
        Student student = getCurrentStudent();
        return evaluationResultRepository
                .findByIdAndStudentIdAndStatusWithContext(resultId, student.getId(), EvaluationResultStatus.PUBLISHED)
                .orElseThrow(() -> new EdusyncException("EVAL-404", "Published result not found", HttpStatus.NOT_FOUND));
    }

    private Map<String, BigDecimal> buildMaxMarksMap(TemplateSnapshot snapshot) {
        if (snapshot == null || snapshot.getSections() == null || snapshot.getSections().isEmpty()) {
            throw new EdusyncException("EVAL-400", "Evaluation structure is unavailable", HttpStatus.BAD_REQUEST);
        }
        Map<String, BigDecimal> maxByKey = new HashMap<>();
        for (TemplateSnapshotSection section : snapshot.getSections()) {
            for (TemplateSnapshotQuestion question : getSnapshotQuestions(section)) {
                BigDecimal max = BigDecimal.valueOf(question.getMarks());
                TemplateQuestionType type = question.getType() == null ? TemplateQuestionType.NORMAL : question.getType();
                if (type == TemplateQuestionType.INTERNAL_CHOICE) {
                    maxByKey.put(buildQuestionKey(section.getName(), question.getQuestionNo(), ""), max);
                    for (String optionLabel : (question.getOptions() == null ? List.<String>of() : question.getOptions())) {
                        maxByKey.put(buildQuestionKey(section.getName(), question.getQuestionNo(), optionLabel), max);
                    }
                } else {
                    maxByKey.put(buildQuestionKey(section.getName(), question.getQuestionNo(), ""), max);
                }
            }
        }
        return maxByKey;
    }

    private String buildQuestionKey(String sectionName, Integer questionNumber, String optionLabel) {
        return sectionName.trim() + "#" + questionNumber + "#" + normalizeOptionLabel(optionLabel);
    }

    private String normalizeOptionLabel(String optionLabel) {
        return optionLabel == null ? "" : optionLabel.trim().toLowerCase(Locale.ROOT);
    }

    private List<SaveQuestionMarkRequestDTO> normalizeQuestionMarks(List<SaveQuestionMarkRequestDTO> questionMarks) {
        Map<String, SaveQuestionMarkRequestDTO> byKey = new LinkedHashMap<>();
        for (SaveQuestionMarkRequestDTO mark : questionMarks) {
            String key = buildQuestionKey(mark.getSectionName(), mark.getQuestionNumber(), mark.getOptionLabel());
            if (byKey.containsKey(key)) {
                throw new EdusyncException("EVAL-400", "Duplicate mark entry for " + key, HttpStatus.BAD_REQUEST);
            }

            if (mark.getMarksObtained() != null && mark.getMarksObtained().compareTo(BigDecimal.ZERO) < 0) {
                throw new EdusyncException("EVAL-400", "marksObtained cannot be negative", HttpStatus.BAD_REQUEST);
            }
            byKey.put(key, mark);
        }
        return new ArrayList<>(byKey.values());
    }

    private List<TemplateSnapshotQuestion> getSnapshotQuestions(TemplateSnapshotSection section) {
        if (section.getQuestions() != null && !section.getQuestions().isEmpty()) {
            return section.getQuestions().stream()
                    .sorted(Comparator.comparing(TemplateSnapshotQuestion::getQuestionNo))
                    .collect(Collectors.toList());
        }

        List<TemplateSnapshotQuestion> generated = new ArrayList<>();
        int total = resolveTotalQuestions(section);
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

    private int resolveTotalQuestions(TemplateSnapshotSection section) {
        return section.getTotalQuestions() != null ? section.getTotalQuestions() : section.getQuestionCount();
    }

    private int resolveAttemptQuestions(TemplateSnapshotSection section) {
        return section.getAttemptQuestions() != null ? section.getAttemptQuestions() : resolveTotalQuestions(section);
    }

    private TemplateSectionType resolveSectionType(TemplateSnapshotSection section) {
        if (section.getSectionType() == null) {
            return TemplateSectionType.FIXED;
        }
        return section.getSectionType();
    }

    private BigDecimal resolveSectionMaxMarks(TemplateSnapshotSection section, List<TemplateSnapshotQuestion> questions) {
        List<Integer> marks = questions.stream()
                .map(TemplateSnapshotQuestion::getMarks)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        if (resolveSectionType(section) == TemplateSectionType.OPTIONAL) {
            return BigDecimal.valueOf(marks.stream().limit(resolveAttemptQuestions(section)).mapToInt(Integer::intValue).sum());
        }
        return BigDecimal.valueOf(marks.stream().mapToInt(Integer::intValue).sum());
    }

    private void markAssignmentInProgress(Long scheduleId, Long teacherId) {
        assignmentRepository.findByExamScheduleIdAndTeacherIdAndRole(
                scheduleId, teacherId, EvaluationAssignmentRole.EVALUATOR
        ).ifPresent(assignment -> {
            if (assignment.getStatus() == EvaluationAssignmentStatus.ASSIGNED) {
                assignment.setStatus(EvaluationAssignmentStatus.IN_PROGRESS);
                assignmentRepository.save(assignment);
            }
        });
    }

    private void markAssignmentCompleted(Long scheduleId, Long teacherId) {
        assignmentRepository.findByExamScheduleIdAndTeacherIdAndRole(
                scheduleId, teacherId, EvaluationAssignmentRole.EVALUATOR
        ).ifPresent(assignment -> {
            assignment.setStatus(EvaluationAssignmentStatus.COMPLETED);
            assignmentRepository.save(assignment);
        });
    }

    private void validateAnnotation(AnnotationRequestDTO requestDTO) {
        if (requestDTO.getPageNumber() == null || requestDTO.getPageNumber() < 1) {
            throw new EdusyncException("EVAL-400", "pageNumber must be >= 1", HttpStatus.BAD_REQUEST);
        }
        if (requestDTO.getX() == null || requestDTO.getY() == null || requestDTO.getX() < 0 || requestDTO.getY() < 0) {
            throw new EdusyncException("EVAL-400", "Annotation coordinates must be non-negative", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (ALLOWED_METADATA_KEYS.contains(entry.getKey())) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        return sanitized.isEmpty() ? null : sanitized;
    }

    private AnnotationResponseDTO toAnnotationResponse(AnswerSheetAnnotation annotation) {
        return AnnotationResponseDTO.builder()
                .id(annotation.getId())
                .pageNumber(annotation.getPageNumber())
                .x(annotation.getX())
                .y(annotation.getY())
                .type(annotation.getType())
                .metadata(annotation.getMetadata())
                .build();
    }

    private String sign(Long answerSheetId, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(fileSigningSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((answerSheetId + ":" + expires).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new EdusyncException("EVAL-500", "Failed to generate signed URL token", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < x.length; i++) {
            result |= x[i] ^ y[i];
        }
        return result == 0;
    }
}

