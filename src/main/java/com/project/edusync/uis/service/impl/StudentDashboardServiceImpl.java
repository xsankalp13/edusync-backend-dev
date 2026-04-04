package com.project.edusync.uis.service.impl;

import com.project.edusync.adm.model.entity.AcademicConstraint;
import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.adm.repository.AcademicConstraintRepository;
import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.ams.model.entity.AttendanceAudit;
import com.project.edusync.ams.model.repository.AttendanceAuditRepository;
import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.StudentMarkRepository;
import com.project.edusync.finance.repository.InvoiceRepository;
import com.project.edusync.uis.model.dto.dashboard.IntelligenceResponseDTO;
import com.project.edusync.uis.model.dto.dashboard.OverviewResponseDTO;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.service.DashboardAggregatorService;
import com.project.edusync.uis.service.StudentDashboardService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentDashboardServiceImpl implements StudentDashboardService, DashboardAggregatorService {

    private static final int DEFAULT_ATTENDANCE_THRESHOLD = 75;

    private final StudentRepository studentRepository;
    private final ScheduleRepository scheduleRepository;
    private final AcademicConstraintRepository academicConstraintRepository;
    private final StudentDailyAttendanceRepository studentDailyAttendanceRepository;
    private final InvoiceRepository invoiceRepository;
    private final AttendanceAuditRepository attendanceAuditRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentMarkRepository studentMarkRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "#userId")
    public IntelligenceResponseDTO getDashboardIntelligence(Long userId, Long academicYearId) {
        long startedAt = System.nanoTime();
        log.info("Starting dashboard aggregation for userId={} academicYearId={}", userId, academicYearId);
        if (academicYearId == null) {
            log.warn("academicYearId is null for userId={}; using current timetable and default aggregation scope", userId);
        }

        Student student = studentRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("No student mapping found for userId: " + userId));
        log.debug("Resolved student mapping studentId={} for userId={}", student.getId(), userId);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<IntelligenceResponseDTO.ProfileDTO> profileFuture =
                    CompletableFuture.supplyAsync(() -> {
                        long t0 = System.nanoTime();
                        IntelligenceResponseDTO.ProfileDTO dto = buildProfile(student);
                        log.debug("Profile segment prepared for studentId={} in {} ms", student.getId(), (System.nanoTime() - t0) / 1_000_000);
                        return dto;
                    }, executor);

            CompletableFuture<IntelligenceResponseDTO.AcademicPulseDTO> academicFuture =
                    CompletableFuture.supplyAsync(() -> {
                        long t0 = System.nanoTime();
                        IntelligenceResponseDTO.AcademicPulseDTO dto = buildAcademicPulse(student, academicYearId);
                        log.debug("Academic pulse segment prepared for studentId={} in {} ms", student.getId(), (System.nanoTime() - t0) / 1_000_000);
                        return dto;
                    }, executor);

            CompletableFuture<IntelligenceResponseDTO.FinanceHealthDTO> financeFuture =
                    CompletableFuture.supplyAsync(() -> {
                        long t0 = System.nanoTime();
                        IntelligenceResponseDTO.FinanceHealthDTO dto = buildFinanceHealth(student.getId());
                        log.debug("Finance segment prepared for studentId={} in {} ms", student.getId(), (System.nanoTime() - t0) / 1_000_000);
                        return dto;
                    }, executor);

            CompletableFuture<IntelligenceResponseDTO.ActivityFeedDTO> activityFuture =
                    CompletableFuture.supplyAsync(() -> {
                        long t0 = System.nanoTime();
                        IntelligenceResponseDTO.ActivityFeedDTO dto = buildActivityFeed(student.getId());
                        log.debug("Activity segment prepared for studentId={} in {} ms", student.getId(), (System.nanoTime() - t0) / 1_000_000);
                        return dto;
                    }, executor);

            CompletableFuture.allOf(profileFuture, academicFuture, financeFuture, activityFuture).join();

            IntelligenceResponseDTO response = new IntelligenceResponseDTO(
                    profileFuture.join(),
                    academicFuture.join(),
                    financeFuture.join(),
                    activityFuture.join()
            );

            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("Completed dashboard aggregation for userId={} studentId={} in {} ms", userId, student.getId(), elapsedMs);
            return response;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardOverview", key = "#userId")
    public OverviewResponseDTO getDashboardOverview(Long userId, Long academicYearId) {
        long startedAt = System.nanoTime();
        log.info("Starting dashboard overview aggregation for userId={} academicYearId={}", userId, academicYearId);

        Student student = studentRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("No student mapping found for userId: " + userId));

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<OverviewResponseDTO.ProfileDTO> profileFuture =
                    CompletableFuture.supplyAsync(() -> buildOverviewProfile(student), executor);

            CompletableFuture<KpiBase> kpiBaseFuture =
                    CompletableFuture.supplyAsync(() -> buildOverviewKpiBase(student.getId()), executor);

            CompletableFuture<List<OverviewResponseDTO.ScheduleItemDTO>> todayScheduleFuture =
                    CompletableFuture.supplyAsync(() -> buildTodaySchedule(student.getSection().getId()), executor);

            CompletableFuture<List<OverviewResponseDTO.PendingAssignmentDTO>> pendingAssignmentsFuture =
                    CompletableFuture.supplyAsync(() -> buildPendingAssignments(student.getSection().getId()), executor);

            CompletableFuture<List<OverviewResponseDTO.PerformanceTrendDTO>> trendFuture =
                    CompletableFuture.supplyAsync(() -> buildPerformanceTrend(student.getId()), executor);

            CompletableFuture<List<OverviewResponseDTO.AnnouncementDTO>> announcementsFuture =
                    CompletableFuture.supplyAsync(() -> buildRecentAnnouncements(student.getId()), executor);

            CompletableFuture.allOf(profileFuture, kpiBaseFuture, todayScheduleFuture, pendingAssignmentsFuture, trendFuture, announcementsFuture).join();

            List<OverviewResponseDTO.PendingAssignmentDTO> pendingAssignments = pendingAssignmentsFuture.join();
            List<OverviewResponseDTO.PerformanceTrendDTO> performanceTrend = trendFuture.join();

            KpiBase kpiBase = kpiBaseFuture.join();
            BigDecimal currentCgpa = performanceTrend.isEmpty()
                    ? BigDecimal.ZERO
                    : performanceTrend.get(performanceTrend.size() - 1).score();

            OverviewResponseDTO.KpisDTO kpis = new OverviewResponseDTO.KpisDTO(
                    kpiBase.attendancePercentage(),
                    currentCgpa,
                    pendingAssignments.size(),
                    kpiBase.totalOverdueFees()
            );

            OverviewResponseDTO response = new OverviewResponseDTO(
                    profileFuture.join(),
                    kpis,
                    todayScheduleFuture.join(),
                    pendingAssignments,
                    performanceTrend,
                    announcementsFuture.join()
            );

            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("Completed dashboard overview aggregation for userId={} studentId={} in {} ms", userId, student.getId(), elapsedMs);
            return response;
        }
    }

    private IntelligenceResponseDTO.ProfileDTO buildProfile(Student student) {
        String middleName = student.getUserProfile().getMiddleName();
        String fullName = String.join(" ",
                safe(student.getUserProfile().getFirstName()),
                safe(middleName),
                safe(student.getUserProfile().getLastName())).trim().replaceAll("\\s+", " ");

        IntelligenceResponseDTO.ProfileDTO dto = new IntelligenceResponseDTO.ProfileDTO(
                student.getId(),
                student.getUserProfile().getUser().getId(),
                fullName,
                student.getEnrollmentNumber(),
                student.getSection().getSectionName(),
                student.getSection().getAcademicClass().getName()
        );
        log.debug("Built profile DTO for studentId={} enrollmentNumber={}", student.getId(), student.getEnrollmentNumber());
        return dto;
    }

    private OverviewResponseDTO.ProfileDTO buildOverviewProfile(Student student) {
        String middleName = student.getUserProfile().getMiddleName();
        String fullName = String.join(" ",
                safe(student.getUserProfile().getFirstName()),
                safe(middleName),
                safe(student.getUserProfile().getLastName())).trim().replaceAll("\\s+", " ");

        String courseOrClass = "%s - Section %s".formatted(
                student.getSection().getAcademicClass().getName(),
                student.getSection().getSectionName()
        );

        return new OverviewResponseDTO.ProfileDTO(
                student.getId(),
                fullName,
                student.getEnrollmentNumber(),
                courseOrClass,
                student.getUserProfile().getProfileUrl()
        );
    }

    private KpiBase buildOverviewKpiBase(Long studentId) {
        BigDecimal attendancePercentage = calculateAttendancePercentage(studentId);
        BigDecimal totalOverdueFees = fetchTotalOverdueFees(studentId);
        return new KpiBase(attendancePercentage, totalOverdueFees);
    }

    @CircuitBreaker(name = "financeDashboard", fallbackMethod = "overdueFeesFallback")
    protected BigDecimal fetchTotalOverdueFees(Long studentId) {
        return invoiceRepository.findTotalOverdueForStudent(studentId);
    }

    @SuppressWarnings("unused")
    protected BigDecimal overdueFeesFallback(Long studentId, Throwable throwable) {
        log.warn("Overdue fee KPI fallback activated for studentId={}", studentId, throwable);
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateAttendancePercentage(Long studentId) {
        long totalClasses = studentDailyAttendanceRepository.countByStudentId(studentId);
        long attendedClasses = studentDailyAttendanceRepository.countPresentByStudentId(studentId);

        if (totalClasses == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(attendedClasses)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalClasses), 2, RoundingMode.HALF_UP);
    }

    private List<OverviewResponseDTO.ScheduleItemDTO> buildTodaySchedule(Long sectionId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        short dayOfWeek = (short) DayOfWeek.from(today).getValue();

        return scheduleRepository.findDaySchedule(sectionId, dayOfWeek).stream()
                .map(schedule -> toOverviewScheduleItem(schedule, today, now))
                .toList();
    }

    private OverviewResponseDTO.ScheduleItemDTO toOverviewScheduleItem(Schedule schedule, LocalDate date, LocalTime now) {
        LocalTime start = schedule.getTimeslot().getStartTime();
        LocalTime end = schedule.getTimeslot().getEndTime();

        OverviewResponseDTO.ScheduleStatus status;
        if (now.isAfter(end)) {
            status = OverviewResponseDTO.ScheduleStatus.COMPLETED;
        } else if ((now.equals(start) || now.isAfter(start)) && now.isBefore(end)) {
            status = OverviewResponseDTO.ScheduleStatus.LIVE;
        } else {
            status = OverviewResponseDTO.ScheduleStatus.UPCOMING;
        }

        String teacherName = fullName(
                schedule.getTeacher().getStaff().getUserProfile().getFirstName(),
                schedule.getTeacher().getStaff().getUserProfile().getMiddleName(),
                schedule.getTeacher().getStaff().getUserProfile().getLastName()
        );

        Room room = schedule.getRoom();
        String roomName = room.getName();

        return new OverviewResponseDTO.ScheduleItemDTO(
                schedule.getId(),
                schedule.getSubject().getName(),
                teacherName,
                roomName,
                date.atTime(start).atZone(ZoneId.systemDefault()).toInstant(),
                date.atTime(end).atZone(ZoneId.systemDefault()).toInstant(),
                status
        );
    }

    private List<OverviewResponseDTO.PendingAssignmentDTO> buildPendingAssignments(Long sectionId) {
        // This endpoint currently derives pending work from upcoming exams until assignment module entities are introduced.
        return examScheduleRepository.findUpcomingForSection(sectionId, LocalDate.now(), PageRequest.of(0, 5)).stream()
                .map(this::toPendingAssignment)
                .toList();
    }

    private OverviewResponseDTO.PendingAssignmentDTO toPendingAssignment(ExamSchedule examSchedule) {
        Instant dueDate = examSchedule.getExamDate()
                .atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant();

        long daysToDue = ChronoUnit.DAYS.between(LocalDate.now(), examSchedule.getExamDate());
        OverviewResponseDTO.AssignmentPriority priority;
        if (daysToDue <= 1) {
            priority = OverviewResponseDTO.AssignmentPriority.HIGH;
        } else if (daysToDue <= 3) {
            priority = OverviewResponseDTO.AssignmentPriority.MEDIUM;
        } else {
            priority = OverviewResponseDTO.AssignmentPriority.LOW;
        }

        return new OverviewResponseDTO.PendingAssignmentDTO(
                examSchedule.getId(),
                examSchedule.getSubject().getName(),
                "Prepare for %s".formatted(examSchedule.getExam().getName()),
                dueDate,
                priority
        );
    }

    private List<OverviewResponseDTO.PerformanceTrendDTO> buildPerformanceTrend(Long studentId) {
        return studentMarkRepository.findPerformanceTrendByStudentId(studentId).stream()
                .map(p -> new OverviewResponseDTO.PerformanceTrendDTO(
                        p.getTerm(),
                        p.getScore() == null
                                ? BigDecimal.ZERO
                                : p.getScore().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private List<OverviewResponseDTO.AnnouncementDTO> buildRecentAnnouncements(Long studentId) {
        return attendanceAuditRepository.findByDailyAttendance_StudentIdOrderByCreatedAtDesc(studentId, PageRequest.of(0, 5)).stream()
                .map(audit -> new OverviewResponseDTO.AnnouncementDTO(
                        audit.getId(),
                        "%s: %s".formatted(audit.getActionType(), safe(audit.getColumnName())),
                        toInstant(audit.getCreatedAt()),
                        OverviewResponseDTO.AnnouncementType.ALERT
                ))
                .toList();
    }

    private IntelligenceResponseDTO.AcademicPulseDTO buildAcademicPulse(Student student, Long academicYearId) {
        // academicYearId is accepted for forward compatibility with year-scoped timetable models.
        IntelligenceResponseDTO.LiveAcademicContextDTO liveContext = buildLiveAcademicContext(student.getSection().getId());
        IntelligenceResponseDTO.PredictiveAttendanceDTO predictiveAttendance = buildPredictiveAttendance(student.getId());
        log.debug("Built academic pulse for studentId={} academicYearId={}", student.getId(), academicYearId);
        return new IntelligenceResponseDTO.AcademicPulseDTO(liveContext, predictiveAttendance);
    }

    private IntelligenceResponseDTO.LiveAcademicContextDTO buildLiveAcademicContext(Long sectionId) {
        LocalDateTime now = LocalDateTime.now();
        short dayOfWeek = (short) DayOfWeek.from(now).getValue();
        LocalTime currentTime = now.toLocalTime();

        Optional<Schedule> currentClass = scheduleRepository.findCurrentClass(sectionId, dayOfWeek, currentTime);
        Optional<Schedule> nextClass = scheduleRepository.findNextClass(sectionId, dayOfWeek, currentTime);

        IntelligenceResponseDTO.LiveAcademicContextDTO dto = new IntelligenceResponseDTO.LiveAcademicContextDTO(
                currentClass.map(this::toClassLabel).orElse(null),
                nextClass.map(this::toClassLabel).orElse(null)
        );
        log.debug("Live academic context computed for sectionId={} currentClassPresent={} nextClassPresent={}",
                sectionId, dto.currentClass() != null, dto.nextClass() != null);
        return dto;
    }

    private IntelligenceResponseDTO.PredictiveAttendanceDTO buildPredictiveAttendance(Long studentId) {
        long totalClasses = studentDailyAttendanceRepository.countByStudentId(studentId);
        long attendedClasses = studentDailyAttendanceRepository.countPresentByStudentId(studentId);

        int threshold = academicConstraintRepository
                .findTopByConstraintTypeIgnoreCaseAndIsActiveTrue("MIN_ATTENDANCE_PERCENT")
                .map(AcademicConstraint::getValueInt)
                .filter(v -> v != null && v > 0)
                .orElse(DEFAULT_ATTENDANCE_THRESHOLD);

        BigDecimal percentage = totalClasses == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(attendedClasses)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalClasses), 2, RoundingMode.HALF_UP);

        IntelligenceResponseDTO.AttendanceStatus status;
        if (percentage.compareTo(BigDecimal.valueOf(threshold)) >= 0) {
            status = IntelligenceResponseDTO.AttendanceStatus.HEALTHY;
        } else if (percentage.compareTo(BigDecimal.valueOf(threshold - 5L)) >= 0) {
            status = IntelligenceResponseDTO.AttendanceStatus.WARNING;
        } else {
            status = IntelligenceResponseDTO.AttendanceStatus.CRITICAL;
        }

        IntelligenceResponseDTO.PredictiveAttendanceDTO dto = new IntelligenceResponseDTO.PredictiveAttendanceDTO(
                totalClasses,
                attendedClasses,
                percentage,
                status,
                threshold
        );
        log.debug("Predictive attendance computed for studentId={} total={} attended={} percentage={} status={}",
                studentId, totalClasses, attendedClasses, percentage, status);
        return dto;
    }

    @CircuitBreaker(name = "financeDashboard", fallbackMethod = "financeFallback")
    protected IntelligenceResponseDTO.FinanceHealthDTO buildFinanceHealth(Long studentId) {
        IntelligenceResponseDTO.FinanceHealthDTO dto = new IntelligenceResponseDTO.FinanceHealthDTO(
                invoiceRepository.findTotalDueForStudent(studentId),
                invoiceRepository.findNextDueDateForStudent(studentId).orElse(null),
                false,
                null
        );
        log.debug("Finance summary computed for studentId={} totalDue={} earliestDueDate={}",
                studentId, dto.totalDue(), dto.earliestDueDate());
        return dto;
    }

    @SuppressWarnings("unused")
    protected IntelligenceResponseDTO.FinanceHealthDTO financeFallback(Long studentId, Throwable throwable) {
        log.warn("Finance segment fallback activated for studentId={}", studentId, throwable);
        return new IntelligenceResponseDTO.FinanceHealthDTO(
                BigDecimal.ZERO,
                null,
                true,
                "Finance data temporarily unavailable"
        );
    }

    private IntelligenceResponseDTO.ActivityFeedDTO buildActivityFeed(Long studentId) {
        List<IntelligenceResponseDTO.RecentActivityDTO> activities = attendanceAuditRepository
                .findByDailyAttendance_StudentIdOrderByCreatedAtDesc(studentId, PageRequest.of(0, 5))
                .stream()
                .map(this::toRecentActivity)
                .toList();

        log.debug("Activity feed computed for studentId={} entries={}", studentId, activities.size());
        return new IntelligenceResponseDTO.ActivityFeedDTO(activities);
    }

    private IntelligenceResponseDTO.RecentActivityDTO toRecentActivity(AttendanceAudit audit) {
        return new IntelligenceResponseDTO.RecentActivityDTO(
                audit.getId(),
                audit.getActionType() == null ? null : audit.getActionType().name(),
                audit.getColumnName(),
                audit.getOldValue(),
                audit.getNewValue(),
                audit.getCreatedAt()
        );
    }

    private String toClassLabel(Schedule schedule) {
        return "%s | %s | %s-%s".formatted(
                schedule.getSubject().getName(),
                schedule.getRoom().getName(),
                schedule.getTimeslot().getStartTime(),
                schedule.getTimeslot().getEndTime()
        );
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private String fullName(String firstName, String middleName, String lastName) {
        return String.join(" ", safe(firstName), safe(middleName), safe(lastName)).trim().replaceAll("\\s+", " ");
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private record KpiBase(BigDecimal attendancePercentage, BigDecimal totalOverdueFees) {}
}

