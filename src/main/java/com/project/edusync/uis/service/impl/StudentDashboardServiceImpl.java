package com.project.edusync.uis.service.impl;

import com.project.edusync.adm.model.entity.AcademicConstraint;
import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.adm.repository.AcademicConstraintRepository;
import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.ams.model.entity.AttendanceAudit;
import com.project.edusync.ams.repository.AttendanceAuditRepository;
import com.project.edusync.ams.repository.StudentDailyAttendanceRepository;
import com.project.edusync.finance.repository.InvoiceRepository;
import com.project.edusync.uis.model.dto.dashboard.IntelligenceResponseDTO;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentDashboardServiceImpl implements StudentDashboardService {

    private static final int DEFAULT_ATTENDANCE_THRESHOLD = 75;

    private final StudentRepository studentRepository;
    private final ScheduleRepository scheduleRepository;
    private final AcademicConstraintRepository academicConstraintRepository;
    private final StudentDailyAttendanceRepository studentDailyAttendanceRepository;
    private final InvoiceRepository invoiceRepository;
    private final AttendanceAuditRepository attendanceAuditRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "#userId")
    public IntelligenceResponseDTO getDashboardIntelligence(Long userId, Long academicYearId) {
        Student student = studentRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("No student mapping found for userId: " + userId));

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<IntelligenceResponseDTO.ProfileDTO> profileFuture =
                    CompletableFuture.supplyAsync(() -> buildProfile(student), executor);

            CompletableFuture<IntelligenceResponseDTO.AcademicPulseDTO> academicFuture =
                    CompletableFuture.supplyAsync(() -> buildAcademicPulse(student, academicYearId), executor);

            CompletableFuture<IntelligenceResponseDTO.FinanceHealthDTO> financeFuture =
                    CompletableFuture.supplyAsync(() -> buildFinanceHealth(student.getId()), executor);

            CompletableFuture<IntelligenceResponseDTO.ActivityFeedDTO> activityFuture =
                    CompletableFuture.supplyAsync(() -> buildActivityFeed(student.getId()), executor);

            CompletableFuture.allOf(profileFuture, academicFuture, financeFuture, activityFuture).join();

            return new IntelligenceResponseDTO(
                    profileFuture.join(),
                    academicFuture.join(),
                    financeFuture.join(),
                    activityFuture.join()
            );
        }
    }

    private IntelligenceResponseDTO.ProfileDTO buildProfile(Student student) {
        String middleName = student.getUserProfile().getMiddleName();
        String fullName = String.join(" ",
                safe(student.getUserProfile().getFirstName()),
                safe(middleName),
                safe(student.getUserProfile().getLastName())).trim().replaceAll("\\s+", " ");

        return new IntelligenceResponseDTO.ProfileDTO(
                student.getId(),
                student.getUserProfile().getUser().getId(),
                fullName,
                student.getEnrollmentNumber(),
                student.getSection().getSectionName(),
                student.getSection().getAcademicClass().getClassName()
        );
    }

    private IntelligenceResponseDTO.AcademicPulseDTO buildAcademicPulse(Student student, Long academicYearId) {
        // academicYearId is accepted for forward compatibility with year-scoped timetable models.
        IntelligenceResponseDTO.LiveAcademicContextDTO liveContext = buildLiveAcademicContext(student.getSection().getId());
        IntelligenceResponseDTO.PredictiveAttendanceDTO predictiveAttendance = buildPredictiveAttendance(student.getId());
        return new IntelligenceResponseDTO.AcademicPulseDTO(liveContext, predictiveAttendance);
    }

    private IntelligenceResponseDTO.LiveAcademicContextDTO buildLiveAcademicContext(Long sectionId) {
        LocalDateTime now = LocalDateTime.now();
        short dayOfWeek = (short) DayOfWeek.from(now).getValue();
        LocalTime currentTime = now.toLocalTime();

        Optional<Schedule> currentClass = scheduleRepository.findCurrentClass(sectionId, dayOfWeek, currentTime);
        Optional<Schedule> nextClass = scheduleRepository.findNextClass(sectionId, dayOfWeek, currentTime);

        return new IntelligenceResponseDTO.LiveAcademicContextDTO(
                currentClass.map(this::toClassLabel).orElse(null),
                nextClass.map(this::toClassLabel).orElse(null)
        );
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

        return new IntelligenceResponseDTO.PredictiveAttendanceDTO(
                totalClasses,
                attendedClasses,
                percentage,
                status,
                threshold
        );
    }

    @CircuitBreaker(name = "financeDashboard", fallbackMethod = "financeFallback")
    protected IntelligenceResponseDTO.FinanceHealthDTO buildFinanceHealth(Long studentId) {
        return new IntelligenceResponseDTO.FinanceHealthDTO(
                invoiceRepository.findTotalDueForStudent(studentId),
                invoiceRepository.findNextDueDateForStudent(studentId).orElse(null),
                false,
                null
        );
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
}

