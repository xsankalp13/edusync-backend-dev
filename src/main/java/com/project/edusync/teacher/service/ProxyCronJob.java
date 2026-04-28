package com.project.edusync.teacher.service;

import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.ams.model.entity.StaffDailyAttendance;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.teacher.model.dto.AdminAssignProxyDto;
import com.project.edusync.teacher.model.enums.ProxyRequestStatus;
import com.project.edusync.teacher.repository.ProxyRequestRepository;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pre-period proxy allocation cron job.
 *
 * <p>Runs every minute and checks for timetable periods starting in the next
 * {@value #ALERT_WINDOW_MINUTES} minutes. For each upcoming period:
 * <ul>
 *   <li>Skips if the assigned teacher has no absence record today (assumed present).</li>
 *   <li>Skips if a proxy is already assigned for this teacher on this date.</li>
 *   <li>If no proxy exists → auto-assigns the least-loaded available teaching staff.</li>
 * </ul>
 *
 * <p>This runs school-wide. Future enhancement: per-school proxy_allocation_mode
 * (AUTO | ADMIN_ASSISTED) stored in school settings.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProxyCronJob {

    /** Minutes before period start to trigger proxy check. */
    private static final int ALERT_WINDOW_MINUTES = 5;

    private static final Set<DayType> NON_WORKING_DAY_TYPES =
            EnumSet.of(DayType.HOLIDAY, DayType.VACATION);

    private final ScheduleRepository scheduleRepository;
    private final StaffDailyAttendanceRepository attendanceRepository;
    private final ProxyRequestRepository proxyRequestRepository;
    private final ProxyRequestService proxyRequestService;
    private final StaffRepository staffRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AcademicCalendarEventRepository calendarEventRepository;

    /**
     * Runs every minute; checks for periods in the next 5 minutes that need proxy cover.
     * Cron: "0 * * * * ?" — top of every minute.
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void checkUpcomingPeriods() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Skip on school holidays / vacations
        if (calendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(
                today, NON_WORKING_DAY_TYPES)) {
            return;
        }

        // Skip on weekends (Saturday=6, Sunday=7 in ISO)
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return;

        // dayOfWeek in Timeslot uses Short (1=Mon…7=Sun matching ISO)
        short dayOfWeek = (short) dow.getValue();

        LocalTime windowStart = now;
        LocalTime windowEnd   = now.plusMinutes(ALERT_WINDOW_MINUTES);

        List<Schedule> upcoming = scheduleRepository
                .findSchedulesStartingBetween(dayOfWeek, windowStart, windowEnd);

        if (upcoming.isEmpty()) return;

        log.debug("ProxyCron: Found {} upcoming periods in next {}m on {}",
                upcoming.size(), ALERT_WINDOW_MINUTES, today);

        for (Schedule schedule : upcoming) {
            processScheduleForProxy(schedule, today);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void processScheduleForProxy(Schedule schedule, LocalDate today) {
        Staff teacherStaff = schedule.getTeacher().getStaff();
        Long staffId = teacherStaff.getId();

        // 1. Check if teacher is absent today
        Optional<StaffDailyAttendance> attendance =
                attendanceRepository.findByStaffIdAndAttendanceDate(staffId, today);

        boolean isAbsent = attendance
                .map(a -> a.getAttendanceType() != null && a.getAttendanceType().isAbsenceMark())
                .orElse(false);

        if (!isAbsent) {
            // Also check approved leave
            boolean onLeave = leaveApplicationRepository.existsOverlapping(
                    staffId, today, today, List.of(LeaveApplicationStatus.APPROVED));
            if (!onLeave) return; // Teacher present — no proxy needed
        }

        Long teacherUserId = teacherStaff.getUser() != null ? teacherStaff.getUser().getId() : null;
        if (teacherUserId == null) return;

        // 2. Check if proxy already assigned for this teacher on today
        boolean alreadyCovered = proxyRequestRepository
                .existsByRequestedByIdAndPeriodDateAndStatusNot(
                        teacherUserId, today, ProxyRequestStatus.CANCELLED);

        if (alreadyCovered) {
            log.debug("ProxyCron: Proxy already exists for staffId={} on {}", staffId, today);
            return;
        }

        // 3. Auto-assign least-loaded available teaching staff
        assignBestProxy(schedule, teacherStaff, today);
    }

    private void assignBestProxy(Schedule schedule, Staff absentStaff, LocalDate today) {
        // Candidate pool: active TEACHING staff (excluding the absent teacher)
        List<Staff> candidates = staffRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.isActive()))
                .filter(s -> s.getCategory() == StaffCategory.TEACHING)
                .filter(s -> !s.getId().equals(absentStaff.getId()))
                .filter(s -> s.getUser() != null)
                .toList();

        if (candidates.isEmpty()) {
            log.warn("ProxyCron: No available proxy candidates for staffId={}", absentStaff.getId());
            return;
        }

        // Score each candidate by weekly proxy count (ascending = least loaded first)
        LocalDate weekStart = today.with(java.time.temporal.TemporalAdjusters
                .previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(java.time.temporal.TemporalAdjusters
                .nextOrSame(DayOfWeek.SUNDAY));

        Staff bestCandidate = candidates.stream()
                .min(Comparator.comparingLong(candidate -> {
                    Long userId = candidate.getUser().getId();
                    return proxyRequestRepository.countAcceptedProxiesBetween(userId, weekStart, weekEnd);
                }))
                .orElse(null);

        if (bestCandidate == null || bestCandidate.getUser() == null) return;

        // Create the proxy request in ACCEPTED state (admin-auto-assigned)
        AdminAssignProxyDto dto = new AdminAssignProxyDto(
                absentStaff.getUser().getUuid(),   // absentStaffUserUuid
                bestCandidate.getUser().getUuid(), // proxyStaffUserUuid
                today,                             // periodDate
                schedule.getSection().getUuid(),   // sectionUuid
                schedule.getSubject().getName()    // subject
        );

        proxyRequestService.adminAssignProxy(dto);

        log.info("ProxyCron: Auto-assigned proxy — absent={} → proxy={} | subject={} | section={} | date={}",
                absentStaff.getId(),
                bestCandidate.getId(),
                schedule.getSubject().getName(),
                schedule.getSection().getUuid(),
                today);
    }
}
