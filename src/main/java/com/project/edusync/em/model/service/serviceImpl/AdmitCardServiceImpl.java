package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.adm.repository.SubjectRepository;
import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.common.settings.service.AppSettingService;
import com.project.edusync.em.model.dto.ResponseDTO.AdmitCardGenerationProgressDTO;
import com.project.edusync.em.model.dto.ResponseDTO.AdmitCardGenerationResponseDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ScheduleAdmitCardStatusDTO;
import com.project.edusync.em.model.dto.internal.admitbatch.AdmitCardData;
import com.project.edusync.em.model.dto.internal.admitbatch.ScheduleDTO;
import com.project.edusync.em.model.dto.internal.admitbatch.SeatDTO;
import com.project.edusync.em.model.dto.internal.admitbatch.StudentDTO;
import com.project.edusync.em.model.entity.Exam;
import com.project.edusync.em.model.repository.AdmitCardRepository;
import com.project.edusync.em.model.repository.ExamRepository;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.SeatAllocationRepository;
import com.project.edusync.em.model.service.AdmitCardService;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.uis.repository.StudentRepository;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmitCardServiceImpl implements AdmitCardService {

    private static final DateTimeFormatter ISSUE_DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final AdmitCardRepository admitCardRepository;
    private final ExamRepository examRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final StudentRepository studentRepository;
    private final PdfGenerationService pdfGenerationService;
    private final AppSettingService appSettingService;
    private final AdmitCardGenerationReadCacheService admitCardGenerationReadCacheService;
    private final SubjectRepository subjectRepository;
    private final com.project.edusync.common.security.AuthUtil authUtil;

    @Resource(name = "admitCardPdfExecutor")
    private Executor admitCardPdfExecutor;

    @Override
    public AdmitCardGenerationResponseDTO generateAdmitCards(UUID examUuid) {
        byte[] pdf = generateBatchAdmitCardsPdf(examUuid, null);
        return buildGenerationResponse(examUuid, null, pdf.length);
    }

    @Override
    public AdmitCardGenerationResponseDTO generateAdmitCardsForSchedule(UUID examUuid, Long scheduleId) {
        byte[] pdf = generateBatchAdmitCardsPdf(examUuid, scheduleId);
        return buildGenerationResponse(examUuid, scheduleId, pdf.length);
    }

    @Override
    public byte[] generateBatchAdmitCardsPdf(UUID examUuid, Long scheduleId) {
        Exam exam = examRepository.findByUuid(examUuid)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam not found", HttpStatus.NOT_FOUND));

        CompletableFuture<List<ExamScheduleRepository.AdmitCardScheduleProjection>> schedulesFuture =
                CompletableFuture.supplyAsync(() -> loadTargetSchedules(exam.getId(), scheduleId), admitCardPdfExecutor);

        CompletableFuture<Map<Long, StudentRepository.AdmitCardStudentProjection>> studentsFuture = schedulesFuture.thenApplyAsync(
                this::fetchStudentsById,
                admitCardPdfExecutor
        );

        CompletableFuture<List<SeatAllocationRepository.AdmitCardSeatAllocationProjection>> seatsFuture = schedulesFuture.thenApplyAsync(
                schedules -> {
                    List<Long> scheduleIds = schedules.stream()
                            .map(ExamScheduleRepository.AdmitCardScheduleProjection::getId)
                            .toList();
                    return scheduleIds.isEmpty() ? List.of() : seatAllocationRepository.findAdmitCardAllocationsByScheduleIds(scheduleIds);
                },
                admitCardPdfExecutor
        );

        CompletableFuture<Map<Long, String>> subjectsFuture = schedulesFuture.thenApplyAsync(
                schedules -> {
                    Set<Long> subjectIds = schedules.stream()
                            .map(ExamScheduleRepository.AdmitCardScheduleProjection::getSubjectId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    if (subjectIds.isEmpty()) {
                        return Map.of();
                    }
                    return subjectRepository.findActiveNamesByIds(subjectIds).stream()
                            .collect(Collectors.toMap(SubjectRepository.SubjectNameProjection::getId,
                                    SubjectRepository.SubjectNameProjection::getName));
                },
                admitCardPdfExecutor
        );

        CompletableFuture.allOf(schedulesFuture, studentsFuture, seatsFuture, subjectsFuture).join();

        List<ExamScheduleRepository.AdmitCardScheduleProjection> schedules = schedulesFuture.join();
        Map<Long, StudentRepository.AdmitCardStudentProjection> studentsById = studentsFuture.join();
        List<SeatAllocationRepository.AdmitCardSeatAllocationProjection> seatAllocations = seatsFuture.join();
        Map<Long, String> subjectNameById = subjectsFuture.join();

        if (studentsById.isEmpty()) {
            throw new EdusyncException("EM-400", "No active students found for selected scope", HttpStatus.BAD_REQUEST);
        }

        Map<String, SeatDTO> seatByScheduleAndStudent = seatAllocations.stream()
                .collect(Collectors.toMap(
                        allocation -> allocationKey(allocation.getScheduleId(), allocation.getStudentId()),
                        allocation -> SeatDTO.builder()
                                .studentId(allocation.getStudentId())
                                .seat(allocation.getSeatLabel())
                                .room(allocation.getRoomName())
                                .build(),
                        (left, right) -> left
                ));

        Map<Long, List<StudentRepository.AdmitCardStudentProjection>> studentsBySchedule = mapStudentsToSchedules(
                schedules,
                studentsById.values()
        );

        // Build a map: studentId → class/section name from the first schedule they appear in
        Map<Long, String[]> studentClassInfo = new HashMap<>();

        Map<Long, AdmitCardData> admitCardDataByStudentId = new LinkedHashMap<>();
        // Intermediate map to collect schedules before building immutable AdmitCardData
        Map<Long, List<ScheduleDTO>> schedulesPerStudent = new LinkedHashMap<>();

        List<ExamScheduleRepository.AdmitCardScheduleProjection> orderedSchedules = schedules.stream()
                .sorted(Comparator.comparing(ExamScheduleRepository.AdmitCardScheduleProjection::getExamDate)
                        .thenComparing(ExamScheduleRepository.AdmitCardScheduleProjection::getStartTime))
                .toList();

        for (ExamScheduleRepository.AdmitCardScheduleProjection schedule : orderedSchedules) {
            String subjectName = subjectNameById.getOrDefault(schedule.getSubjectId(), schedule.getSubjectName());
            for (StudentRepository.AdmitCardStudentProjection student : studentsBySchedule.getOrDefault(schedule.getId(), List.of())) {
                // Remember class/section from first schedule the student appears in
                studentClassInfo.putIfAbsent(student.getId(), new String[]{
                        schedule.getAcademicClassName() != null ? schedule.getAcademicClassName() : "N/A",
                        schedule.getSectionName() != null ? schedule.getSectionName() : "N/A"
                });

                schedulesPerStudent.computeIfAbsent(student.getId(), k -> new ArrayList<>());

                SeatDTO seat = seatByScheduleAndStudent.get(allocationKey(schedule.getId(), student.getId()));
                schedulesPerStudent.get(student.getId()).add(ScheduleDTO.builder()
                        .subject(subjectName != null ? subjectName : "N/A")
                        .date(schedule.getExamDate())
                        .startTime(schedule.getStartTime())
                        .endTime(schedule.getEndTime())
                        .seat(seat != null && seat.getSeat() != null ? seat.getSeat() : "Not Allocated")
                        .room(seat != null && seat.getRoom() != null ? seat.getRoom() : "Not Allocated")
                        .build());
            }
        }

        // Build final card data with all per-card metadata
        String examTypeName = exam.getExamType() != null ? exam.getExamType().name() : "";
        String issueDateStr = LocalDateTime.now().format(ISSUE_DATE_TIME_FMT);

        for (Map.Entry<Long, List<ScheduleDTO>> entry : schedulesPerStudent.entrySet()) {
            Long studentId = entry.getKey();
            StudentRepository.AdmitCardStudentProjection studentProj = studentsById.get(studentId);
            if (studentProj == null) continue;

            String[] classInfo = studentClassInfo.getOrDefault(studentId, new String[]{"N/A", "N/A"});
            StudentDTO studentDTO = toStudentDTO(studentProj, classInfo[0], classInfo[1]);

            String admitCardNumber = "AC-" + exam.getId() + "-" + studentId;
            String verificationCode = admitCardNumber;
            String qrCodeBase64;
            String qrText = admitCardNumber + "|" + studentId + "|" + exam.getId();
            try {
                qrCodeBase64 = pdfGenerationService.generateQrCodeBase64(qrText, 100);
            } catch (Exception e) {
                qrCodeBase64 = "";
            }

            admitCardDataByStudentId.put(studentId, AdmitCardData.builder()
                    .student(studentDTO)
                    .schedules(entry.getValue())
                    .admitCardNumber(admitCardNumber)
                    .qrCodeBase64(qrCodeBase64)
                    .verificationCode(verificationCode)
                    .examType(examTypeName)
                    .issueDate(issueDateStr)
                    .build());
        }

        List<AdmitCardData> cards = admitCardDataByStudentId.values().stream()
                .sorted(Comparator.comparing((AdmitCardData card) -> card.getStudent().getRollNo(), Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(card -> card.getStudent().getName(), Comparator.nullsLast(String::compareTo)))
                .toList();

        if (cards.isEmpty()) {
            throw new EdusyncException("EM-400", "No admit card data could be assembled — no students matched schedules", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> templateData = new HashMap<>();
        populateSchoolBrandingData(templateData);
        templateData.put("examName", exam.getName());
        templateData.put("academicYear", exam.getAcademicYear());
        templateData.put("examType", examTypeName);
        templateData.put("examSessionTitle", exam.getName() + " (" + exam.getAcademicYear() + ")");
        templateData.put("generatedAt", issueDateStr);
        templateData.put("cards", cards);

        return pdfGenerationService.generatePdfFromHtml("em/admit-card-batch", templateData);
    }

    @Override
    public List<ScheduleAdmitCardStatusDTO> getAdmitCardStatusByExam(UUID examUuid) {
        Exam exam = examRepository.findByUuid(examUuid)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam not found", HttpStatus.NOT_FOUND));

        List<ExamScheduleRepository.AdmitCardScheduleProjection> schedules = admitCardGenerationReadCacheService
                .getAdmitCardSchedules(exam.getId());
        if (schedules.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> generatedPerSchedule = new HashMap<>();
        for (Object[] row : admitCardRepository.countGeneratedPerSchedule(exam.getId())) {
            generatedPerSchedule.put((Long) row[0], ((Long) row[1]).intValue());
        }

        Map<Long, Integer> publishedPerSchedule = new HashMap<>();
        for (Object[] row : admitCardRepository.countPublishedPerSchedule(exam.getId())) {
            publishedPerSchedule.put((Long) row[0], ((Long) row[1]).intValue());
        }

        List<ScheduleAdmitCardStatusDTO> result = new ArrayList<>(schedules.size());
        for (ExamScheduleRepository.AdmitCardScheduleProjection schedule : schedules) {
            int totalStudents = Math.max(schedule.getActiveStudentCount() == null ? 0 : schedule.getActiveStudentCount(), 0);
            int generated = generatedPerSchedule.getOrDefault(schedule.getId(), 0);
            int published = publishedPerSchedule.getOrDefault(schedule.getId(), 0);
            result.add(ScheduleAdmitCardStatusDTO.builder()
                    .scheduleId(schedule.getId())
                    .className(schedule.getAcademicClassName())
                    .sectionName(schedule.getSectionName())
                    .subjectName(schedule.getSubjectName())
                    .examDate(schedule.getExamDate() != null ? schedule.getExamDate().toString() : null)
                    .totalStudents(totalStudents)
                    .generatedCount(generated)
                    .allGenerated(totalStudents > 0 && generated >= totalStudents)
                    .publishedCount(published)
                    .allPublished(totalStudents > 0 && published >= totalStudents)
                    .build());
        }
        return result;
    }

    @Override
    public AdmitCardGenerationProgressDTO getGenerationProgress(UUID examUuid) {
        Exam exam = examRepository.findByUuid(examUuid)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam not found", HttpStatus.NOT_FOUND));

        int total = admitCardGenerationReadCacheService.getAdmitCardSchedules(exam.getId()).stream()
                .mapToInt(schedule -> Math.max(schedule.getActiveStudentCount() == null ? 0 : schedule.getActiveStudentCount(), 0))
                .sum();
        int generated = 0;
        int published = 0;
        int failed = 0;
        for (Object[] row : admitCardRepository.countByStatusForExam(exam.getId())) {
            String status = String.valueOf(row[0]);
            int count = ((Long) row[1]).intValue();
            if ("PUBLISHED".equals(status)) {
                published = count;
                generated += count;
            } else if ("GENERATED".equals(status)) {
                generated += count;
            } else if ("FAILED".equals(status)) {
                failed = count;
            }
        }
        int percentage = total == 0 ? 0 : Math.min((int) (((generated + failed) * 100.0f) / total), 100);

        return AdmitCardGenerationProgressDTO.builder()
                .total(total)
                .generated(generated)
                .published(published)
                .failed(failed)
                .percentage(percentage)
                .build();
    }

    private AdmitCardGenerationResponseDTO buildGenerationResponse(UUID examUuid, Long scheduleId, int pdfBytes) {
        Exam exam = examRepository.findByUuid(examUuid)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam not found", HttpStatus.NOT_FOUND));
        int generatedCount;
        if (scheduleId == null) {
            generatedCount = admitCardGenerationReadCacheService.getAdmitCardSchedules(exam.getId()).stream()
                    .mapToInt(schedule -> Math.max(schedule.getActiveStudentCount() == null ? 0 : schedule.getActiveStudentCount(), 0))
                    .sum();
        } else {
            generatedCount = admitCardGenerationReadCacheService.getAdmitCardSchedules(exam.getId()).stream()
                    .filter(schedule -> schedule.getId().equals(scheduleId))
                    .mapToInt(schedule -> Math.max(schedule.getActiveStudentCount() == null ? 0 : schedule.getActiveStudentCount(), 0))
                    .findFirst()
                    .orElse(0);
        }
        return AdmitCardGenerationResponseDTO.builder()
                .examId(exam.getId())
                .examName(exam.getName())
                .generatedCount(generatedCount)
                .generatedAt(LocalDateTime.now())
                .message("Batch admit card PDF generated successfully (bytes=" + pdfBytes + ")")
                .build();
    }

    @Override
    public com.project.edusync.em.model.dto.ResponseDTO.AdmitCardResponseDTO getStudentAdmitCard(UUID examUuid) {
        Exam exam = examRepository.findByUuid(examUuid)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(exam.getTimetablePublished())) {
            throw new EdusyncException("EM-403", "Timetable is not published yet", HttpStatus.FORBIDDEN);
        }

        Long userId = authUtil.getCurrentUserId();
        com.project.edusync.uis.model.entity.Student student = studentRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new EdusyncException("EVAL-403", "Current user is not mapped to student", HttpStatus.FORBIDDEN));

        java.util.Optional<com.project.edusync.em.model.entity.AdmitCard> admitCardOpt =
                admitCardRepository.findByExam_IdAndStudent_Id(exam.getId(), student.getId());

        if (admitCardOpt.isPresent()) {
            return toAdmitCardResponseDTO(admitCardOpt.get());
        } else {
            return buildDynamicScheduleResponse(exam, student);
        }
    }

    private com.project.edusync.em.model.dto.ResponseDTO.AdmitCardResponseDTO toAdmitCardResponseDTO(com.project.edusync.em.model.entity.AdmitCard ac) {
        List<com.project.edusync.em.model.dto.ResponseDTO.AdmitCardEntryResponseDTO> entries = ac.getEntries().stream()
            .map(e -> com.project.edusync.em.model.dto.ResponseDTO.AdmitCardEntryResponseDTO.builder()
                .examScheduleId(e.getExamSchedule().getId())
                .subjectId(e.getExamSchedule().getSubject().getId())
                .subjectName(e.getExamSchedule().getSubject().getName())
                .examDate(e.getExamDate())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .roomId(0L)
                .roomName(e.getRoomName() != null ? e.getRoomName() : "TBA")
                .seatId(0L)
                .seatLabel(e.getSeatLabel() != null ? e.getSeatLabel() : "TBA")
                .build())
            .collect(Collectors.toList());

        String fullName = (ac.getStudent().getUserProfile().getFirstName() + " " + ac.getStudent().getUserProfile().getLastName()).trim();
        return com.project.edusync.em.model.dto.ResponseDTO.AdmitCardResponseDTO.builder()
                .admitCardId(ac.getId())
                .examId(ac.getExam().getId())
                .examName(ac.getExam().getName())
                .studentId(ac.getStudent().getUuid())
                .studentName(fullName)
                .enrollmentNumber(ac.getStudent().getEnrollmentNumber())
                .status(ac.getStatus().name())
                .pdfUrl(ac.getPdfUrl())
                .generatedAt(ac.getGeneratedAt() != null ? ac.getGeneratedAt() : LocalDateTime.now())
                .entries(entries)
                .build();
    }

    private com.project.edusync.em.model.dto.ResponseDTO.AdmitCardResponseDTO buildDynamicScheduleResponse(Exam exam, com.project.edusync.uis.model.entity.Student student) {
        List<com.project.edusync.em.model.entity.ExamSchedule> applicableSchedules = new ArrayList<>();
        if (exam.getSchedules() != null) {
            for (com.project.edusync.em.model.entity.ExamSchedule schedule : exam.getSchedules()) {
                if (schedule.getSection() != null) {
                    if (student.getSection() != null && schedule.getSection().getId().equals(student.getSection().getId())) {
                        applicableSchedules.add(schedule);
                    }
                } else if (schedule.getAcademicClass() != null) {
                    if (student.getSection() != null && student.getSection().getAcademicClass() != null &&
                            schedule.getAcademicClass().getId().equals(student.getSection().getAcademicClass().getId())) {
                        applicableSchedules.add(schedule);
                    }
                }
            }
        }

        List<Long> scheduleIds = applicableSchedules.stream().map(com.project.edusync.em.model.entity.ExamSchedule::getId).collect(Collectors.toList());
        List<SeatAllocationRepository.AdmitCardSeatAllocationProjection> seatAllocations = new ArrayList<>();
        if (!scheduleIds.isEmpty()) {
            seatAllocations = seatAllocationRepository.findAdmitCardAllocationsByScheduleIdsAndStudentIds(
                    scheduleIds, List.of(student.getId()));
        }

        Map<Long, SeatAllocationRepository.AdmitCardSeatAllocationProjection> allocationByScheduleId = seatAllocations.stream()
                .collect(Collectors.toMap(SeatAllocationRepository.AdmitCardSeatAllocationProjection::getScheduleId, a -> a, (a, b) -> a));

        List<com.project.edusync.em.model.dto.ResponseDTO.AdmitCardEntryResponseDTO> entries = new ArrayList<>();
        for (com.project.edusync.em.model.entity.ExamSchedule schedule : applicableSchedules) {
            SeatAllocationRepository.AdmitCardSeatAllocationProjection allocation = allocationByScheduleId.get(schedule.getId());
            Long roomId = allocation != null ? allocation.getRoomId() : 0L;
            String roomName = allocation != null ? allocation.getRoomName() : "TBA";
            Long seatId = allocation != null ? allocation.getSeatId() : 0L;
            String seatLabel = allocation != null ? allocation.getSeatLabel() : "TBA";

            entries.add(com.project.edusync.em.model.dto.ResponseDTO.AdmitCardEntryResponseDTO.builder()
                    .examScheduleId(schedule.getId())
                    .subjectId(schedule.getSubject().getId())
                    .subjectName(schedule.getSubject().getName())
                    .examDate(schedule.getExamDate())
                    .startTime(schedule.getTimeslot() != null ? schedule.getTimeslot().getStartTime() : null)
                    .endTime(schedule.getTimeslot() != null ? schedule.getTimeslot().getEndTime() : null)
                    .roomId(roomId)
                    .roomName(roomName)
                    .seatId(seatId)
                    .seatLabel(seatLabel)
                    .build());
        }

        String fullName = (student.getUserProfile().getFirstName() + " " + student.getUserProfile().getLastName()).trim();
        return com.project.edusync.em.model.dto.ResponseDTO.AdmitCardResponseDTO.builder()
                .admitCardId(0L)
                .examId(exam.getId())
                .examName(exam.getName())
                .studentId(student.getUuid())
                .studentName(fullName)
                .enrollmentNumber(student.getEnrollmentNumber())
                .status("PUBLISHED")
                .generatedAt(LocalDateTime.now())
                .entries(entries)
                .build();
    }

    private List<ExamScheduleRepository.AdmitCardScheduleProjection> loadTargetSchedules(Long examId, Long scheduleId) {
        List<ExamScheduleRepository.AdmitCardScheduleProjection> schedules = admitCardGenerationReadCacheService.getAdmitCardSchedules(examId);
        if (scheduleId == null) {
            if (schedules.isEmpty()) {
                throw new EdusyncException("EM-400", "No exam schedules found for admit card generation", HttpStatus.BAD_REQUEST);
            }
            return schedules;
        }
        return schedules.stream()
                .filter(schedule -> schedule.getId().equals(scheduleId))
                .findFirst()
                .map(List::of)
                .orElseThrow(() -> new EdusyncException("EM-404", "Schedule not found or does not belong to this exam", HttpStatus.NOT_FOUND));
    }

    private Map<Long, StudentRepository.AdmitCardStudentProjection> fetchStudentsById(
            List<ExamScheduleRepository.AdmitCardScheduleProjection> schedules) {
        Set<Long> sectionIds = schedules.stream()
                .map(ExamScheduleRepository.AdmitCardScheduleProjection::getSectionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> classIds = schedules.stream()
                .filter(schedule -> schedule.getSectionId() == null)
                .map(ExamScheduleRepository.AdmitCardScheduleProjection::getAcademicClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, StudentRepository.AdmitCardStudentProjection> studentsById = new LinkedHashMap<>();
        if (!sectionIds.isEmpty()) {
            for (StudentRepository.AdmitCardStudentProjection student :
                    studentRepository.findActiveAdmitCardStudentsBySectionIds(new ArrayList<>(sectionIds))) {
                studentsById.put(student.getId(), student);
            }
        }
        if (!classIds.isEmpty()) {
            for (StudentRepository.AdmitCardStudentProjection student :
                    studentRepository.findActiveAdmitCardStudentsByClassIds(new ArrayList<>(classIds))) {
                studentsById.put(student.getId(), student);
            }
        }
        return studentsById;
    }

    private Map<Long, List<StudentRepository.AdmitCardStudentProjection>> mapStudentsToSchedules(
            List<ExamScheduleRepository.AdmitCardScheduleProjection> schedules,
            Collection<StudentRepository.AdmitCardStudentProjection> students) {
        Map<Long, List<StudentRepository.AdmitCardStudentProjection>> studentsBySection = students.stream()
                .filter(student -> student.getSectionId() != null)
                .collect(Collectors.groupingBy(StudentRepository.AdmitCardStudentProjection::getSectionId));

        Map<Long, List<StudentRepository.AdmitCardStudentProjection>> studentsByClass = students.stream()
                .filter(student -> student.getClassId() != null)
                .collect(Collectors.groupingBy(StudentRepository.AdmitCardStudentProjection::getClassId));

        Map<Long, List<StudentRepository.AdmitCardStudentProjection>> studentsBySchedule = new HashMap<>();
        for (ExamScheduleRepository.AdmitCardScheduleProjection schedule : schedules) {
            if (schedule.getSectionId() != null) {
                studentsBySchedule.put(schedule.getId(), studentsBySection.getOrDefault(schedule.getSectionId(), List.of()));
            } else {
                studentsBySchedule.put(schedule.getId(), studentsByClass.getOrDefault(schedule.getAcademicClassId(), List.of()));
            }
        }
        return studentsBySchedule;
    }

    private StudentDTO toStudentDTO(StudentRepository.AdmitCardStudentProjection projection,
                                     String className, String sectionName) {
        String firstName = projection.getFirstName() != null ? projection.getFirstName() : "";
        String lastName = projection.getLastName() != null ? projection.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return StudentDTO.builder()
                .id(projection.getId())
                .name(fullName.isEmpty() ? "N/A" : fullName)
                .rollNo(projection.getRollNo())
                .enrollmentNumber(projection.getEnrollmentNumber() != null ? projection.getEnrollmentNumber() : "")
                .className(className != null ? className : "N/A")
                .sectionName(sectionName != null ? sectionName : "N/A")
                .photoBase64(null)
                .build();
    }

    private void populateSchoolBrandingData(Map<String, Object> data) {
        String schoolName = appSettingService.getValue("school.name", "My School");
        String shortName = appSettingService.getValue("school.short_name", "");
        data.put("schoolName", schoolName);
        data.put("schoolShortName", shortName.isBlank() ? schoolName : shortName);
        data.put("schoolTagline", appSettingService.getValue("school.tagline", ""));
        data.put("schoolAddress", appSettingService.getValue("school.address", ""));
        data.put("schoolPhone", appSettingService.getValue("school.phone", ""));
        data.put("schoolEmail", appSettingService.getValue("school.email", ""));

        String headerMode = appSettingService.getValue("school.id_card_header_mode", "TEXT");
        String headerImageUrl = appSettingService.getValue("school.id_card_header_image_url", "");
        String headerImageBase64 = "";
        if ("IMAGE".equalsIgnoreCase(headerMode) && !headerImageUrl.isBlank()) {
            headerImageBase64 = pdfGenerationService.fetchRemoteImageAsBase64OrEmpty(headerImageUrl);
        }
        data.put("headerImageEnabled", !headerImageBase64.isBlank());
        data.put("headerImageBase64", headerImageBase64);

        String logoUrl = appSettingService.getValue("school.logo_url", "");
        if (!logoUrl.isBlank()) {
            data.put("schoolLogoBase64", pdfGenerationService.fetchRemoteImageAsBase64(logoUrl));
        } else {
            data.put("schoolLogoBase64", pdfGenerationService.loadSchoolLogoBase64());
        }
    }

    private String allocationKey(Long scheduleId, Long studentId) {
        return scheduleId + "#" + studentId;
    }
}
