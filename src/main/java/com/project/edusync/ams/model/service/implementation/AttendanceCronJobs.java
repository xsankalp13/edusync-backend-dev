package com.project.edusync.ams.model.service.implementation;

import com.project.edusync.ams.model.entity.AttendanceType;
import com.project.edusync.ams.model.entity.StaffDailyAttendance;
import com.project.edusync.ams.model.entity.StaffShiftMapping;
import com.project.edusync.ams.model.enums.AttendanceSource;
import com.project.edusync.ams.model.repository.AttendanceTypeRepository;
import com.project.edusync.ams.model.repository.LateClockInRequestRepository;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.ams.model.repository.StaffShiftMappingRepository;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceCronJobs {

    private static final Set<DayType> NON_WORKING_DAY_TYPES = EnumSet.of(DayType.HOLIDAY, DayType.VACATION);

    private final StaffShiftMappingRepository staffShiftMappingRepository;
    private final StaffDailyAttendanceRepository staffDailyAttendanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AttendanceTypeRepository attendanceTypeRepository;
    private final AcademicCalendarEventRepository academicCalendarEventRepository;
    private final LateClockInRequestRepository lateClockInRequestRepository;

    /**
     * Nightly batch processor to resolve End-of-Day Attendance exceptions.
     * Runs every day at 11:30 PM (23:30).
     * <p>
     * Responsibilities:
     * 1. Auto-mark ABSENT for staff with no check-in.
     * 2. Flag missing out-punch records.
     * 3. Create LateClockInRequest entries for staff who clocked in beyond maxLateThreshold.
     */
    @Scheduled(cron = "0 30 23 * * ?")
    @Transactional
    public void processEndOfDayAttendance() {
        log.info("CronJob: Starting End of Day Staff Attendance checks.");
        LocalDate today = LocalDate.now();

        if (academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(today, NON_WORKING_DAY_TYPES)) {
            log.info("CronJob: Skipping End of Day Staff Attendance checks due to holiday/vacation calendar event on {}.", today);
            return;
        }

        // Retrieve the standard 'Absent' Attendance Type via predefined ShortCode (often 'A')
        Optional<AttendanceType> absentTypeOpt = attendanceTypeRepository.findByShortCodeIgnoreCase("A");

        if (absentTypeOpt.isEmpty()) {
            log.error("CronJob: Missing AttendanceType with shortCode 'A'. Cannot auto-mark absentees.");
            return;
        }

        AttendanceType absentType = absentTypeOpt.get();

        // 1. Fetch all active shift mappings for today
        List<StaffShiftMapping> activeMappings = staffShiftMappingRepository.findAll().stream()
                .filter(m -> !m.getEffectiveFrom().isAfter(today) && 
                            (m.getEffectiveTo() == null || !m.getEffectiveTo().isBefore(today)))
                .toList();

        for (StaffShiftMapping mapping : activeMappings) {
            Long staffId = mapping.getStaff().getId();

            // 2. Check for overlapping Approved Leaves today
            boolean existsApprovedLeave = leaveApplicationRepository.existsOverlapping(
                    staffId,
                    today,
                    today,
                    List.of(LeaveApplicationStatus.APPROVED)
            );

            if (existsApprovedLeave) {
                log.debug("CronJob: StaffId {} is on approved leave. Skipping absent rule.", staffId);
                continue; // Skip any attendance processing for this employee
            }

            // 3. Evaluate Daily Check-ins
            Optional<StaffDailyAttendance> dailyRecordOpt =
                    staffDailyAttendanceRepository.findByStaffIdAndAttendanceDate(staffId, today);

            if (dailyRecordOpt.isEmpty()) {
                // 3a. NO check-in -> Create an 'Absent' record.
                StaffDailyAttendance absentRecord = new StaffDailyAttendance();
                absentRecord.setStaffId(staffId);
                absentRecord.setAttendanceDate(today);
                absentRecord.setAttendanceType(absentType);
                absentRecord.setSource(AttendanceSource.SYSTEM);
                absentRecord.setNotes("SYSTEM WARNING: Auto-marked as Absent for not checking in.");
                
                staffDailyAttendanceRepository.save(absentRecord);
                log.info("CronJob: Auto-marked StaffId {} as Absent.", staffId);

            } else {
                // 3b. Missing Out-Punch -> Append Warning Exception
                StaffDailyAttendance record = dailyRecordOpt.get();

                if (record.getTimeIn() != null && record.getTimeOut() == null) {
                    String missingSwipeMsg = "SYSTEM WARNING: Missing Out Punch. Requires School Admin Review.";

                    if (record.getNotes() == null || !record.getNotes().contains("Missing Out Punch")) {
                        String newNotes = (record.getNotes() == null || record.getNotes().isBlank())
                                ? missingSwipeMsg
                                : record.getNotes() + " | " + missingSwipeMsg;
                        
                        // Limit size to max length 500 equivalent if needed
                        if (newNotes.length() > 500) {
                            newNotes = newNotes.substring(0, 497) + "...";
                        }
                        
                        record.setNotes(newNotes);
                        staffDailyAttendanceRepository.save(record);
                        log.info("CronJob: Flagged StaffId {} for missing checkout.", staffId);
                    }
                }

                // 3c. Late Clock-In Review Queue
                // If clocked-in AND beyond the shift's maxLateThreshold → create LateClockInRequest.
                flagLateClockIn(staffId, today, record, mapping);
            }
        }
        
        log.info("CronJob: Finished End of Day Staff Attendance checks.");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private void flagLateClockIn(Long staffId, LocalDate date,
                                  StaffDailyAttendance record, StaffShiftMapping mapping) {
        if (record.getTimeIn() == null || mapping.getShift() == null) return;

        var shift = mapping.getShift();
        int grace = shift.getGraceMinutes() != null ? shift.getGraceMinutes() : 0;

        long totalShiftMinutes = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
        if (totalShiftMinutes < 0) totalShiftMinutes += 1440;

        long maxLate = shift.getMaxLateThresholdMinutes() != null
                ? shift.getMaxLateThresholdMinutes()
                : totalShiftMinutes / 2;

        long minutesLate = Duration.between(shift.getStartTime(), record.getTimeIn()).toMinutes();
        if (minutesLate < 0) minutesLate += 1440;
        minutesLate = Math.max(0, minutesLate - grace);

        if (minutesLate <= maxLate) return; // Not late enough to warrant review
        if (lateClockInRequestRepository.existsByStaffIdAndAttendanceDate(staffId, date)) return; // Already logged

        var lateReq = new com.project.edusync.ams.model.entity.LateClockInRequest();
        lateReq.setStaffId(staffId);
        lateReq.setAttendanceDate(date);
        lateReq.setClockInTime(record.getTimeIn());
        lateReq.setMinutesLate((int) minutesLate);
        lateReq.setAttendance(record);
        lateClockInRequestRepository.save(lateReq);

        log.info("CronJob: Created LateClockInRequest for StaffId {} — {} min late on {}.",
                staffId, minutesLate, date);
    }
}
