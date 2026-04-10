package com.project.edusync.teacher.service.impl;

import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.model.entity.Timeslot;
import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.adm.repository.TimeslotRepository;
import com.project.edusync.ams.model.entity.StudentDailyAttendance;
import com.project.edusync.teacher.service.TeacherDashboardService;
import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.teacher.model.dto.TeacherDashboardSummaryResponseDto;
import com.project.edusync.teacher.model.dto.TeacherHomeroomResponseDto;
import com.project.edusync.teacher.model.dto.TeacherMyClassesResponseDto;
import com.project.edusync.teacher.model.dto.TeacherScheduleResponseDto;
import com.project.edusync.teacher.model.dto.TeacherStudentResponseDto;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.StudentGuardianRelationship;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentGuardianRelationshipRepository;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherDashboardServiceImpl implements TeacherDashboardService {

    private final StudentDailyAttendanceRepository studentDailyAttendanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final TimeslotRepository timeslotRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final StudentGuardianRelationshipRepository studentGuardianRelationshipRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherMyClassesResponseDto> getMyClasses(Long currentUserId) {
        Staff staff = resolveStaffFromCurrentUser(currentUserId);
        List<Schedule> schedules = scheduleRepository.findAllActiveByTeacherStaffIdWithReferences(staff.getId());

        Map<UUID, TeacherClassAccumulator> bySection = new LinkedHashMap<>();
        for (Schedule schedule : schedules) {
            Section section = schedule.getSection();
            UUID sectionUuid = section.getUuid();
            TeacherClassAccumulator acc = bySection.computeIfAbsent(sectionUuid, ignored -> new TeacherClassAccumulator(section));
            acc.subjectsByUuid.putIfAbsent(
                    schedule.getSubject().getUuid(),
                    TeacherMyClassesResponseDto.SubjectItem.builder()
                            .subjectUuid(schedule.getSubject().getUuid())
                            .subjectName(schedule.getSubject().getName())
                            .subjectCode(schedule.getSubject().getSubjectCode())
                            .build()
            );
        }

        bySection.values().forEach(acc -> acc.studentCount = studentRepository.countBySection_IdAndIsActiveTrue(acc.section.getId()));

        return bySection.values().stream()
                .map(acc -> TeacherMyClassesResponseDto.builder()
                        .classUuid(acc.section.getAcademicClass().getUuid())
                        .className(acc.section.getAcademicClass().getName())
                        .sectionUuid(acc.section.getUuid())
                        .sectionName(acc.section.getSectionName())
                        .isClassTeacher(isClassTeacher(staff.getId(), acc.section))
                        .subjects(acc.subjectsByUuid.values().stream()
                                .sorted(Comparator.comparing(TeacherMyClassesResponseDto.SubjectItem::getSubjectName, String.CASE_INSENSITIVE_ORDER))
                                .toList())
                        .studentCount(acc.studentCount)
                        .build())
                .sorted(Comparator
                        .comparing(TeacherMyClassesResponseDto::isClassTeacher).reversed()
                        .thenComparing(TeacherMyClassesResponseDto::getClassName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(TeacherMyClassesResponseDto::getSectionName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherMyClassesResponseDto> getMyClassTeacherSections(Long currentUserId) {
        Staff staff = resolveStaffFromCurrentUser(currentUserId);
        List<Section> sections = sectionRepository.findAllActiveByClassTeacherId(staff.getId());

        return sections.stream().map(section -> {
                    List<Schedule> schedules = scheduleRepository.findAllActiveByTeacherStaffIdAndSectionId(staff.getId(), section.getId());
                    Map<UUID, TeacherMyClassesResponseDto.SubjectItem> subjectsByUuid = new LinkedHashMap<>();
                    for (Schedule schedule : schedules) {
                        subjectsByUuid.putIfAbsent(
                                schedule.getSubject().getUuid(),
                                TeacherMyClassesResponseDto.SubjectItem.builder()
                                        .subjectUuid(schedule.getSubject().getUuid())
                                        .subjectName(schedule.getSubject().getName())
                                        .subjectCode(schedule.getSubject().getSubjectCode())
                                        .build()
                        );
                    }

                    long studentCount = studentRepository.countBySection_IdAndIsActiveTrue(section.getId());
                    return TeacherMyClassesResponseDto.builder()
                            .classUuid(section.getAcademicClass().getUuid())
                            .className(section.getAcademicClass().getName())
                            .sectionUuid(section.getUuid())
                            .sectionName(section.getSectionName())
                            .isClassTeacher(true)
                            .subjects(subjectsByUuid.values().stream()
                                    .sorted(Comparator.comparing(TeacherMyClassesResponseDto.SubjectItem::getSubjectName, String.CASE_INSENSITIVE_ORDER))
                                    .toList())
                            .studentCount(studentCount)
                            .build();
                })
                .sorted(Comparator
                        .comparing(TeacherMyClassesResponseDto::getClassName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(TeacherMyClassesResponseDto::getSectionName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherStudentResponseDto> getMyStudents(Long currentUserId,
                                                         UUID classUuid,
                                                         UUID sectionUuid,
                                                         String search,
                                                         Pageable pageable) {
        Staff staff = resolveStaffFromCurrentUser(currentUserId);
        List<Long> teacherSectionIds = scheduleRepository.findDistinctActiveSectionIdsByTeacherStaffId(staff.getId());
        if (teacherSectionIds.isEmpty()) {
            return Page.empty(pageable);
        }

        boolean searchEnabled = search != null && !search.isBlank();
        String searchPattern = toLikePattern(search);
        Page<Student> page = studentRepository.findTeacherStudents(
                teacherSectionIds,
                classUuid,
                sectionUuid,
                searchEnabled,
                searchPattern,
                pageable
        );

        return mapStudents(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherStudentResponseDto> getClassTeacherStudents(Long currentUserId,
                                                                   UUID sectionUuid,
                                                                   String search,
                                                                   Pageable pageable) {
        Staff staff = resolveStaffFromCurrentUser(currentUserId);
        Section section = sectionRepository.findByUuid(sectionUuid)
                .orElseThrow(() -> new EdusyncException("Section not found", HttpStatus.NOT_FOUND));

        if (!isClassTeacher(staff.getId(), section)) {
            throw new EdusyncException("You are not the class teacher for this section", HttpStatus.FORBIDDEN);
        }

        boolean searchEnabled = search != null && !search.isBlank();
        String searchPattern = toLikePattern(search);
        Page<Student> page = studentRepository.findTeacherStudents(
                List.of(section.getId()),
                null,
                sectionUuid,
                searchEnabled,
                searchPattern,
                pageable
        );

        return mapStudents(page);
    }

    private Page<TeacherStudentResponseDto> mapStudents(Page<Student> page) {

        List<Long> studentIds = page.getContent().stream().map(Student::getId).toList();
        Map<Long, StudentDailyAttendanceRepository.StudentAttendanceAggregateProjection> attendanceByStudent =
                attendanceSummaryByStudent(studentIds, currentAcademicStart(LocalDate.now()), LocalDate.now());

        Map<Long, StudentGuardianRelationship> guardianByStudent = studentGuardianRelationshipRepository
                .findPrimaryContactsByStudentIds(studentIds)
                .stream()
                .collect(Collectors.toMap(rel -> rel.getStudent().getId(), Function.identity(), (first, ignored) -> first));

        return page.map(student -> {
            StudentDailyAttendanceRepository.StudentAttendanceAggregateProjection attendance = attendanceByStudent.get(student.getId());
            long present = attendance == null ? 0L : safe(attendance.getPresentCount());
            long absent = attendance == null ? 0L : safe(attendance.getAbsentCount());
            long total = attendance == null ? 0L : safe(attendance.getTotalCount());
            BigDecimal percentage = percent(present, total);

            StudentGuardianRelationship guardianRelationship = guardianByStudent.get(student.getId());
            String guardianName = null;
            String guardianPhone = null;
            if (guardianRelationship != null && guardianRelationship.getGuardian() != null) {
                guardianName = fullName(
                        guardianRelationship.getGuardian().getUserProfile().getFirstName(),
                        guardianRelationship.getGuardian().getUserProfile().getLastName()
                );
                guardianPhone = guardianRelationship.getGuardian().getPhoneNumber();
            }

            return TeacherStudentResponseDto.builder()
                    .uuid(student.getUuid())
                    .firstName(student.getUserProfile().getFirstName())
                    .lastName(student.getUserProfile().getLastName())
                    .profileUrl(student.getUserProfile().getProfileUrl())
                    .enrollmentNo(student.getEnrollmentNumber())
                    .rollNumber(student.getRollNo() == null ? null : String.valueOf(student.getRollNo()))
                    .className(student.getSection().getAcademicClass().getName())
                    .sectionName(student.getSection().getSectionName())
                    .classUuid(student.getSection().getAcademicClass().getUuid())
                    .sectionUuid(student.getSection().getUuid())
                    .guardianName(guardianName)
                    .guardianPhone(guardianPhone)
                    .attendancePercentage(percentage)
                    .totalPresent(present)
                    .totalAbsent(absent)
                    .totalWorkingDays(total)
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherScheduleResponseDto getMySchedule(Long currentUserId, LocalDate date) {
        Staff staff = resolveStaffFromCurrentUser(currentUserId);
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LocalDate weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(5);

        List<Timeslot> weekTimeslots = timeslotRepository.findAllActive().stream()
                .filter(t -> t.getDayOfWeek() != null)
                .filter(t -> {
                    DayOfWeek day = mapDayOfWeek(t.getDayOfWeek());
                    return day != null && EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY).contains(day);
                })
                .sorted(Comparator
                        .comparing((Timeslot t) -> mapDayOfWeek(t.getDayOfWeek()))
                        .thenComparing(Timeslot::getStartTime))
                .toList();

        Map<Long, Schedule> scheduleByTimeslotId = scheduleRepository.findAllActiveByTeacherStaffIdWithReferences(staff.getId())
                .stream()
                .collect(Collectors.toMap(s -> s.getTimeslot().getId(), Function.identity(), (first, ignored) -> first));

        List<TeacherScheduleResponseDto.Entry> entries = new ArrayList<>();
        for (Timeslot timeslot : weekTimeslots) {
            Schedule schedule = scheduleByTimeslotId.get(timeslot.getId());
            TeacherScheduleResponseDto.SlotType slotType;
            if (Boolean.TRUE.equals(timeslot.getIsBreak())) {
                slotType = TeacherScheduleResponseDto.SlotType.BREAK;
            } else if (schedule != null) {
                slotType = TeacherScheduleResponseDto.SlotType.TEACHING;
            } else {
                slotType = TeacherScheduleResponseDto.SlotType.LEISURE;
            }

            entries.add(TeacherScheduleResponseDto.Entry.builder()
                    .scheduleEntryUuid(schedule == null ? null : schedule.getUuid())
                    .dayOfWeek(mapDayOfWeek(timeslot.getDayOfWeek()).name())
                    .slotType(slotType)
                    .timeslot(TeacherScheduleResponseDto.TimeslotItem.builder()
                            .uuid(timeslot.getUuid())
                            .startTime(timeslot.getStartTime())
                            .endTime(timeslot.getEndTime())
                            .slotLabel(timeslot.getSlotLabel())
                            .isBreak(Boolean.TRUE.equals(timeslot.getIsBreak()))
                            .build())
                    .subject(schedule == null ? null : TeacherScheduleResponseDto.SubjectItem.builder()
                            .uuid(schedule.getSubject().getUuid())
                            .subjectName(schedule.getSubject().getName())
                            .subjectCode(schedule.getSubject().getSubjectCode())
                            .build())
                    .clazz(schedule == null ? null : TeacherScheduleResponseDto.ClassItem.builder()
                            .uuid(schedule.getSection().getAcademicClass().getUuid())
                            .className(schedule.getSection().getAcademicClass().getName())
                            .build())
                    .section(schedule == null ? null : TeacherScheduleResponseDto.SectionItem.builder()
                            .uuid(schedule.getSection().getUuid())
                            .sectionName(schedule.getSection().getSectionName())
                            .build())
                    .room(schedule == null ? null : TeacherScheduleResponseDto.RoomItem.builder()
                            .uuid(schedule.getRoom().getUuid())
                            .roomName(schedule.getRoom().getName())
                            .roomType(schedule.getRoom().getRoomType())
                            .floor(formatFloor(schedule.getRoom().getFloorNumber()))
                            .build())
                    .build());
        }

        return TeacherScheduleResponseDto.builder()
                .teacherName(fullName(staff.getUserProfile().getFirstName(), staff.getUserProfile().getLastName()))
                .staffUuid(staff.getUuid())
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .entries(entries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "teacherDashboardSummaryV2", key = "#currentUserId + ':' + (#date == null ? T(java.time.LocalDate).now().toString() : #date.toString())")
    public TeacherDashboardSummaryResponseDto getDashboardSummary(Long currentUserId, LocalDate date) {
        Staff staff = resolveStaffFromCurrentUser(currentUserId);
        LocalDate targetDate = date == null ? LocalDate.now() : date;

        boolean isOnLeaveToday = leaveApplicationRepository.existsOverlapping(
                staff.getId(),
                targetDate,
                targetDate,
                List.of(LeaveApplicationStatus.APPROVED)
        );

        List<Long> sectionIds = scheduleRepository.findDistinctActiveSectionIdsByTeacherStaffId(staff.getId());
        if (sectionIds.isEmpty()) {
            return emptySummary(targetDate, isOnLeaveToday);
        }

        List<Student> students = studentRepository.findTeacherStudents(sectionIds, null, null, false, "%", Pageable.unpaged()).getContent();
        List<Long> studentIds = students.stream().map(Student::getId).toList();
        long totalStudents = studentIds.size();

        Map<Long, StudentDailyAttendanceRepository.StudentAttendanceAggregateProjection> today =
                attendanceSummaryByStudent(studentIds, targetDate, targetDate);
        long present = today.values().stream().mapToLong(p -> safe(p.getPresentCount())).sum();
        long absent = today.values().stream().mapToLong(p -> safe(p.getAbsentCount())).sum();
        long late = today.values().stream().mapToLong(p -> safe(p.getLateCount())).sum();
        long marked = today.values().stream().mapToLong(p -> safe(p.getTotalCount())).sum();

        List<Schedule> teacherSchedules = scheduleRepository.findAllActiveByTeacherStaffIdWithReferences(staff.getId());
        DayOfWeek targetDay = targetDate.getDayOfWeek();
        long classesToday = teacherSchedules.stream()
                .filter(s -> mapDayOfWeek(s.getTimeslot().getDayOfWeek()) == targetDay)
                .count();

        Map<Long, StudentDailyAttendanceRepository.StudentAttendanceAggregateProjection> ytd =
                attendanceSummaryByStudent(studentIds, currentAcademicStart(targetDate), targetDate);
        long atRisk = ytd.values().stream().filter(p -> percentAsDouble(safe(p.getPresentCount()), safe(p.getTotalCount())) < 75.0d).count();
        long belowThreshold = ytd.values().stream().filter(p -> percentAsDouble(safe(p.getPresentCount()), safe(p.getTotalCount())) < 90.0d).count();

        TeacherDashboardSummaryResponseDto.NextClass nextClass = findNextClass(teacherSchedules, targetDate);

        return TeacherDashboardSummaryResponseDto.builder()
                .date(targetDate)
                .totalStudents(totalStudents)
                .classesToday(classesToday)
                .attendance(TeacherHomeroomResponseDto.TodayAttendance.builder()
                        .present(present)
                        .absent(absent)
                        .late(late)
                        .notMarked(Math.max(totalStudents - marked, 0L))
                        .percentage(percent(present, totalStudents))
                        .attendanceMarkedToday(marked > 0)
                        .build())
                .alerts(TeacherDashboardSummaryResponseDto.Alerts.builder()
                        .atRiskStudentCount(atRisk)
                        .pendingLeaveRequests(leaveApplicationRepository.countByActiveTrueAndStatus(LeaveApplicationStatus.PENDING))
                        .belowThresholdCount(belowThreshold)
                        .build())
                .nextClass(nextClass)
                .isOnLeaveToday(isOnLeaveToday)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherHomeroomResponseDto getMyHomeroom(Long currentUserId, LocalDate date) {
        Staff staff = resolveStaffFromCurrentUser(currentUserId);
        LocalDate targetDate = date == null ? LocalDate.now() : date;

        Optional<Section> homeroomOpt = sectionRepository.findActiveHomeroomByClassTeacherId(staff.getId());
        if (homeroomOpt.isEmpty()) {
            return TeacherHomeroomResponseDto.builder().isClassTeacher(false).build();
        }

        Section homeroom = homeroomOpt.get();
        List<Student> students = studentRepository.findAllBySectionIdWithDetails(homeroom.getId());
        List<Long> studentIds = students.stream().map(Student::getId).toList();

        Map<Long, StudentDailyAttendanceRepository.StudentAttendanceAggregateProjection> today =
                attendanceSummaryByStudent(studentIds, targetDate, targetDate);
        long present = today.values().stream().mapToLong(p -> safe(p.getPresentCount())).sum();
        long absent = today.values().stream().mapToLong(p -> safe(p.getAbsentCount())).sum();
        long late = today.values().stream().mapToLong(p -> safe(p.getLateCount())).sum();
        long marked = today.values().stream().mapToLong(p -> safe(p.getTotalCount())).sum();

        LocalDate yearStart = currentAcademicStart(targetDate);
        Map<Long, StudentDailyAttendanceRepository.StudentAttendanceAggregateProjection> ytd =
                attendanceSummaryByStudent(studentIds, yearStart, targetDate);

        List<TeacherHomeroomResponseDto.AtRiskStudent> atRiskStudents = students.stream()
                .map(student -> {
                    StudentDailyAttendanceRepository.StudentAttendanceAggregateProjection agg = ytd.get(student.getId());
                    long p = agg == null ? 0L : safe(agg.getPresentCount());
                    long t = agg == null ? 0L : safe(agg.getTotalCount());
                    BigDecimal pct = percent(p, t);
                    long consecutiveAbsences = consecutiveAbsences(student.getId());
                    return TeacherHomeroomResponseDto.AtRiskStudent.builder()
                            .studentUuid(student.getUuid())
                            .name(fullName(student.getUserProfile().getFirstName(), student.getUserProfile().getLastName()))
                            .attendancePercentage(pct)
                            .consecutiveAbsences(consecutiveAbsences)
                            .build();
                })
                .filter(item -> item.getAttendancePercentage().compareTo(BigDecimal.valueOf(75)) < 0 || item.getConsecutiveAbsences() >= 3)
                .sorted(Comparator
                        .comparing(TeacherHomeroomResponseDto.AtRiskStudent::getAttendancePercentage)
                        .thenComparing(TeacherHomeroomResponseDto.AtRiskStudent::getConsecutiveAbsences, Comparator.reverseOrder()))
                .limit(10)
                .toList();

        return TeacherHomeroomResponseDto.builder()
                .isClassTeacher(true)
                .classUuid(homeroom.getAcademicClass().getUuid())
                .className(homeroom.getAcademicClass().getName())
                .sectionUuid(homeroom.getUuid())
                .sectionName(homeroom.getSectionName())
                .defaultRoom(homeroom.getDefaultRoom() == null ? null : TeacherHomeroomResponseDto.DefaultRoom.builder()
                        .uuid(homeroom.getDefaultRoom().getUuid())
                        .roomName(homeroom.getDefaultRoom().getName())
                        .build())
                .studentCount(students.size())
                .todayAttendance(TeacherHomeroomResponseDto.TodayAttendance.builder()
                        .present(present)
                        .absent(absent)
                        .late(late)
                        .notMarked(Math.max(students.size() - marked, 0L))
                        .percentage(percent(present, students.size()))
                        .attendanceMarkedToday(marked > 0)
                        .build())
                .atRiskStudents(atRiskStudents)
                .build();
    }

    private Staff resolveStaffFromCurrentUser(Long currentUserId) {
        return staffRepository.findByUserProfile_User_Id(currentUserId)
                .orElseThrow(() -> new EdusyncException("Authenticated user is not linked to a staff profile", HttpStatus.FORBIDDEN));
    }

    private boolean isClassTeacher(Long staffId, Section section) {
        return section.getClassTeacher() != null && staffId.equals(section.getClassTeacher().getId());
    }

    private String toLikePattern(String value) {
        if (value == null || value.isBlank()) {
            return "%";
        }
        return "%" + value.trim().toLowerCase() + "%";
    }

    private Map<Long, StudentDailyAttendanceRepository.StudentAttendanceAggregateProjection> attendanceSummaryByStudent(
            List<Long> studentIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }
        return studentDailyAttendanceRepository
                .summarizeAttendanceForStudents(studentIds, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(StudentDailyAttendanceRepository.StudentAttendanceAggregateProjection::getStudentId, Function.identity()));
    }

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private double percentAsDouble(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0d;
        }
        return (numerator * 100.0d) / denominator;
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private DayOfWeek mapDayOfWeek(Short dayOfWeekCode) {
        if (dayOfWeekCode == null) {
            return null;
        }
        int value = dayOfWeekCode;
        if (value >= 1 && value <= 7) {
            return DayOfWeek.of(value);
        }
        if (value == 0) {
            return DayOfWeek.SUNDAY;
        }
        return null;
    }

    private String fullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }

    private String formatFloor(Integer floorNumber) {
        if (floorNumber == null) {
            return null;
        }
        return floorNumber + ordinalSuffix(floorNumber) + " Floor";
    }

    private String ordinalSuffix(int value) {
        int mod100 = value % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return "th";
        }
        return switch (value % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    private TeacherDashboardSummaryResponseDto.NextClass findNextClass(List<Schedule> schedules, LocalDate date) {
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        LocalTime now = LocalDate.now().equals(date) ? LocalTime.now() : LocalTime.MIN;
        return schedules.stream()
                .filter(s -> mapDayOfWeek(s.getTimeslot().getDayOfWeek()) == dayOfWeek)
                .filter(s -> s.getTimeslot().getStartTime().isAfter(now))
                .sorted(Comparator.comparing(s -> s.getTimeslot().getStartTime()))
                .findFirst()
                .map(s -> TeacherDashboardSummaryResponseDto.NextClass.builder()
                        .subject(s.getSubject().getName())
                        .className(s.getSection().getAcademicClass().getName())
                        .sectionName(s.getSection().getSectionName())
                        .room(s.getRoom().getName())
                        .startTime(s.getTimeslot().getStartTime())
                        .endTime(s.getTimeslot().getEndTime())
                        .build())
                .orElse(null);
    }

    private LocalDate currentAcademicStart(LocalDate date) {
        return LocalDate.of(date.getYear(), 1, 1);
    }

    private long consecutiveAbsences(Long studentId) {
        List<StudentDailyAttendance> latest = studentDailyAttendanceRepository
                .findByStudentIdOrderByAttendanceDateDesc(studentId, Pageable.ofSize(15))
                .getContent();
        long consecutive = 0L;
        for (StudentDailyAttendance attendance : latest) {
            if (!attendance.getAttendanceType().isAbsenceMark()) {
                break;
            }
            consecutive++;
        }
        return consecutive;
    }

    private TeacherDashboardSummaryResponseDto emptySummary(LocalDate date, boolean isOnLeave) {
        return TeacherDashboardSummaryResponseDto.builder()
                .date(date)
                .totalStudents(0)
                .classesToday(0)
                .attendance(TeacherHomeroomResponseDto.TodayAttendance.builder()
                        .present(0)
                        .absent(0)
                        .late(0)
                        .notMarked(0)
                        .percentage(BigDecimal.ZERO)
                        .attendanceMarkedToday(false)
                        .build())
                .alerts(TeacherDashboardSummaryResponseDto.Alerts.builder()
                        .atRiskStudentCount(0)
                        .pendingLeaveRequests(0)
                        .belowThresholdCount(0)
                        .build())
                .nextClass(null)
                .isOnLeaveToday(isOnLeave)
                .build();
    }

    private static class TeacherClassAccumulator {
        private final Section section;
        private final Map<UUID, TeacherMyClassesResponseDto.SubjectItem> subjectsByUuid = new LinkedHashMap<>();
        private long studentCount;

        private TeacherClassAccumulator(Section section) {
            this.section = section;
        }
    }
}