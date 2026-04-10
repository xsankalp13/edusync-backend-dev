package com.project.edusync.ams.model.service.implementation;

import com.project.edusync.ams.model.dto.request.StaffAttendanceRequestDTO;
import com.project.edusync.ams.model.dto.response.AttendanceCompletionDTO;
import com.project.edusync.ams.model.dto.response.StaffDailyStatsResponseDTO;
import com.project.edusync.ams.model.dto.response.StaffAttendanceResponseDTO;
import com.project.edusync.ams.model.entity.AttendanceType;
import com.project.edusync.ams.model.entity.StaffDailyAttendance;
import com.project.edusync.ams.model.enums.AttendanceSource;
import com.project.edusync.ams.model.exception.AttendanceProcessingException;
import com.project.edusync.ams.model.exception.AttendanceRecordNotFoundException;
import com.project.edusync.ams.model.repository.AttendanceTypeRepository;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.ams.model.service.AttendanceEditWindowService;
import com.project.edusync.ams.model.service.GeoFenceValidator;
import com.project.edusync.ams.model.service.StaffAttendanceService;
import com.project.edusync.uis.model.dto.admin.StaffSummaryDTO;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.stream.Stream;
import java.util.*;
import java.util.stream.Collectors;

import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.ams.model.repository.StaffShiftMappingRepository;
import com.project.edusync.ams.model.entity.StaffShiftMapping;
import com.project.edusync.ams.model.entity.ShiftDefinition;
import com.project.edusync.ams.model.repository.ShiftDefinitionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    private static final Set<DayType> NON_WORKING_DAY_TYPES = EnumSet.of(DayType.HOLIDAY, DayType.VACATION);

    private final StaffDailyAttendanceRepository repo;
    private final AttendanceTypeRepository attendanceTypeRepo;
    private final StaffRepository staffRepository;
    private final GeoFenceValidator geoFenceValidator;
    private final AttendanceEditWindowService attendanceEditWindowService;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final StaffShiftMappingRepository staffShiftMappingRepository;
    private final ShiftDefinitionRepository shiftDefinitionRepository;
    private final AcademicCalendarEventRepository academicCalendarEventRepository;

    /* -------------------------------------------------------------
     * CREATE / UPSERT
     * ------------------------------------------------------------- */
    @Override
    @Transactional
    public StaffAttendanceResponseDTO createAttendance(StaffAttendanceRequestDTO req, Long performedBy) {

        validateTimes(req.getTimeIn(), req.getTimeOut());
        Long staffId = resolveStaffId(req);

        if (isNonWorkingStaffDay(req.getAttendanceDate())) {
            if (req.getSource() != AttendanceSource.MANUAL) {
                throw new IllegalArgumentException("Cannot clock in on an official holiday.");
            }
            throw new IllegalArgumentException("Cannot mark staff attendance on a holiday or vacation date.");
        }

        AttendanceType at;
        if (req.getSource() != AttendanceSource.MANUAL) {
            // Block if on leave
            boolean isOnLeave = leaveApplicationRepository.existsOverlapping(
                    staffId,
                    req.getAttendanceDate(),
                    req.getAttendanceDate(),
                    List.of(LeaveApplicationStatus.APPROVED)
            );
            if (isOnLeave) {
                throw new AttendanceProcessingException("Check-in blocked. You are on an approved leave today. Please contact School Admin if this is an error.");
            }
        }

        at = resolveAttendanceType(staffId, req.getAttendanceDate(), req.getTimeIn(), req.getAttendanceShortCode());

        Optional<StaffDailyAttendance> existing =
                repo.findByStaffIdAndAttendanceDate(staffId, req.getAttendanceDate());

        StaffDailyAttendance e = existing.orElseGet(StaffDailyAttendance::new);
        e.setStaffId(resolveStaffId(req));
        e.setAttendanceDate(req.getAttendanceDate());
        e.setAttendanceType(at);
        e.setTimeIn(req.getTimeIn());
        e.setTimeOut(req.getTimeOut());
        e.setTotalHours(req.getTotalHours());
        e.setSource(req.getSource());
        e.setNotes(req.getNotes());
        e.setLatitude(req.getLatitude());
        e.setLongitude(req.getLongitude());
        boolean geoVerifiedByCoordinates = geoFenceValidator.verifyByCoordinatesIfPresent(req.getLatitude(), req.getLongitude());
        e.setGeoVerified(geoVerifiedByCoordinates || geoFenceValidator.validateAndResolveGeoVerified(req, performedBy, e.getStaffId()));
        applyEarlyClockOutFlag(e.getStaffId(), req.getAttendanceDate(), req.getTimeOut(), e);

        StaffDailyAttendance saved = repo.save(e);
        return toDto(saved);
    }

    /* -------------------------------------------------------------
     * BULK CREATE / UPSERT
     * ------------------------------------------------------------- */
    @Override
    @Transactional
    public List<StaffAttendanceResponseDTO> bulkCreate(List<StaffAttendanceRequestDTO> requests, Long performedBy) {
        if (requests == null || requests.isEmpty()) return Collections.emptyList();

        Optional<LocalDate> blockedDate = requests.stream()
                .map(StaffAttendanceRequestDTO::getAttendanceDate)
                .filter(Objects::nonNull)
                .filter(this::isNonWorkingStaffDay)
                .findFirst();

        if (blockedDate.isPresent()) {
            throw new IllegalArgumentException("Cannot bulk mark staff attendance on a holiday or vacation date: " + blockedDate.get());
        }

        Set<String> codes = requests.stream()
                .map(r -> r.getAttendanceShortCode().trim().toUpperCase())
                .collect(Collectors.toSet());

        Map<String, AttendanceType> types = new HashMap<>();
        for (String code : codes) {
            attendanceTypeRepo.findByShortCodeIgnoreCase(code)
                    .ifPresent(t -> types.put(code, t));
        }

        Set<String> missing = new HashSet<>(codes);
        missing.removeAll(types.keySet());
        if (!missing.isEmpty()) {
            throw new AttendanceProcessingException("Unknown attendance type(s): " + missing);
        }

        List<StaffDailyAttendance> saved = new ArrayList<>();

        for (StaffAttendanceRequestDTO r : requests) {
            validateTimes(r.getTimeIn(), r.getTimeOut());

            AttendanceType at = types.get(r.getAttendanceShortCode().trim().toUpperCase());
            Long staffId = resolveStaffId(r);
            Optional<StaffDailyAttendance> existing =
                    repo.findByStaffIdAndAttendanceDate(staffId, r.getAttendanceDate());

            StaffDailyAttendance e = existing.orElseGet(StaffDailyAttendance::new);
            e.setStaffId(staffId);
            e.setAttendanceDate(r.getAttendanceDate());
            e.setAttendanceType(at);
            e.setTimeIn(r.getTimeIn());
            e.setTimeOut(r.getTimeOut());
            e.setTotalHours(r.getTotalHours());
            e.setSource(r.getSource());
            e.setNotes(r.getNotes());
            e.setLatitude(r.getLatitude());
            e.setLongitude(r.getLongitude());
            e.setGeoVerified(geoFenceValidator.validateAndResolveGeoVerified(r, performedBy, e.getStaffId()));
            applyEarlyClockOutFlag(e.getStaffId(), r.getAttendanceDate(), r.getTimeOut(), e);

            saved.add(repo.save(e));
        }

        return saved.stream().map(this::toDto).toList();
    }

    /* -------------------------------------------------------------
     * LIST FILTERED
     * ------------------------------------------------------------- */
    @Override
    public Page<StaffAttendanceResponseDTO> listAttendances(Pageable pageable,
                                                            Optional<UUID> staffUuid,
                                                            Optional<LocalDate> date,
                                                            Optional<LocalDate> fromDate,
                                                            Optional<LocalDate> toDate,
                                                            Optional<String> status,
                                                            Optional<String> search) {

        Optional<Long> staffId = staffUuid.map(this::resolveStaffIdFromUuid);

        // Extract effective date bounds
        LocalDate effectiveFrom = date.orElse(fromDate.orElse(null));
        LocalDate effectiveTo = date.orElse(toDate.orElse(null));

        Specification<StaffDailyAttendance> spec = (root, query, cb) -> cb.conjunction();

        if (staffId.isPresent()) {
            Long id = staffId.get();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("staffId"), id));
        }
        if (date.isPresent()) {
            LocalDate d = date.get();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("attendanceDate"), d));
        }
        if (fromDate.isPresent()) {
            LocalDate d = fromDate.get();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("attendanceDate"), d));
        }
        if (toDate.isPresent()) {
            LocalDate d = toDate.get();
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("attendanceDate"), d));
        }

        if (status.isPresent()) {
            String sc = status.get().trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("attendanceType").get("shortCode")), sc));
        }

        if (search.isPresent()) {
            List<Long> matchingStaffIds = staffRepository.findStaffIdsBySearch(search.get().trim());
            if (matchingStaffIds.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            spec = spec.and((root, query, cb) -> root.get("staffId").in(matchingStaffIds));
        }

        Page<StaffDailyAttendance> page = repo.findAll(spec, pageable);
        
        List<StaffAttendanceResponseDTO> dtos = new java.util.ArrayList<>(page.getContent().stream()
                .map(this::toDto)
                .peek(dto -> {
                    // Inject real shift boundaries if staff context is resolved
                    staffId.flatMap(sfId -> staffShiftMappingRepository.findCurrentMappingsByStaffId(sfId, dto.getAttendanceDate() != null ? dto.getAttendanceDate() : LocalDate.now()).stream().findFirst())
                            .ifPresent(mapping -> {
                                dto.setShiftStartTime(mapping.getShift().getStartTime());
                                dto.setShiftEndTime(mapping.getShift().getEndTime());
                                dto.setShiftGraceMinutes(mapping.getShift().getGraceMinutes());
                            });
                }).toList());

        // Hydrate synthetic Leave records
        if (staffId.isPresent() && effectiveFrom != null && effectiveTo != null) {
            List<com.project.edusync.hrms.model.entity.LeaveApplication> leaves = leaveApplicationRepository.findApprovedActiveByStaffIdAndDateRange(
                    staffId.get(), effectiveFrom, effectiveTo
            );

            // Dynamically assign shift timings to leaves if available
            LocalTime leaveShiftStartTime = null;
            LocalTime leaveShiftEndTime = null;
            Integer leaveShiftGrace = null;
            
            var shiftOpt = staffShiftMappingRepository.findCurrentMappingsByStaffId(staffId.get(), LocalDate.now()).stream().findFirst();
            if (shiftOpt.isPresent()) {
                leaveShiftStartTime = shiftOpt.get().getShift().getStartTime();
                leaveShiftEndTime = shiftOpt.get().getShift().getEndTime();
                leaveShiftGrace = shiftOpt.get().getShift().getGraceMinutes();
            }

            for (com.project.edusync.hrms.model.entity.LeaveApplication la : leaves) {
                LocalDate curDate = la.getFromDate().isBefore(effectiveFrom) ? effectiveFrom : la.getFromDate();
                LocalDate maxDate = la.getToDate().isAfter(effectiveTo) ? effectiveTo : la.getToDate();

                while (!curDate.isAfter(maxDate)) {
                    // Make sure we haven't already marked attendance physically for this date
                    final LocalDate dt = curDate;
                    if (dtos.stream().noneMatch(d -> d.getAttendanceDate().equals(dt))) {
                        StaffAttendanceResponseDTO lvDto = new StaffAttendanceResponseDTO();
                        lvDto.setUuid(java.util.UUID.randomUUID().toString());
                        lvDto.setStaffUuid(staffUuid.get().toString());
                        lvDto.setAttendanceDate(curDate);
                        lvDto.setAttendanceMark("Leave");
                        lvDto.setShortCode("LV");
                        lvDto.setColorCode("#8B5CF6");
                        lvDto.setSource(AttendanceSource.SYSTEM);
                        lvDto.setNotes("Generated via Approved Leave Application");
                        lvDto.setShiftStartTime(leaveShiftStartTime);
                        lvDto.setShiftEndTime(leaveShiftEndTime);
                        lvDto.setShiftGraceMinutes(leaveShiftGrace);
                        dtos.add(lvDto);
                    }
                    curDate = curDate.plusDays(1);
                }
            }

            // Ensure chronologically sorted
            dtos.sort(java.util.Comparator.comparing(StaffAttendanceResponseDTO::getAttendanceDate));
        }

        return new PageImpl<>(dtos, pageable, page.getTotalElements() + (dtos.size() - page.getContent().size()));
    }

    @Override
    public StaffDailyStatsResponseDTO getDailyStats(Optional<LocalDate> date) {
        LocalDate targetDate = date.orElse(LocalDate.now());

        if (isNonWorkingStaffDay(targetDate)) {
            return StaffDailyStatsResponseDTO.builder()
                    .date(targetDate)
                    .totalMarked(0L)
                    .present(0L)
                    .absent(0L)
                    .late(0L)
                    .onLeave(0L)
                    .unmarkedCount(0L)
                    .build();
        }

        Map<String, Long> countsByShortCode = repo.countByDateGroupedByShortCode(targetDate).stream()
                .collect(Collectors.toMap(
                        row -> Optional.ofNullable(row.getShortCode()).orElse("").toUpperCase(),
                        row -> Optional.ofNullable(row.getCount()).orElse(0L),
                        Long::sum
                ));

        long totalMarked = countsByShortCode.values().stream().mapToLong(Long::longValue).sum();
        long totalExpectedStaffForDate = resolveExpectedStaffByDate(
                staffRepository.findAll().stream().filter(Staff::isActive).toList(),
                targetDate
        ).size();

        return StaffDailyStatsResponseDTO.builder()
                .date(targetDate)
                .totalMarked(totalMarked)
                .present(countsByShortCode.getOrDefault("P", 0L))
                .absent(countsByShortCode.getOrDefault("A", 0L))
                .late(countsByShortCode.getOrDefault("L", 0L))
                .onLeave(countsByShortCode.getOrDefault("LV", 0L))
                .unmarkedCount(Math.max(0L, totalExpectedStaffForDate - totalMarked))
                .build();
    }

    /* -------------------------------------------------------------
     * GET ONE
     * ------------------------------------------------------------- */
    @Override
    public StaffAttendanceResponseDTO getAttendance(UUID recordUuid) {
        StaffDailyAttendance e = repo.findByUuid(recordUuid)
                .orElseThrow(() ->
                        new AttendanceRecordNotFoundException("Staff attendance not found: " + recordUuid)
                );
        return toDto(e);
    }

    /* -------------------------------------------------------------
     * UPDATE
     * ------------------------------------------------------------- */
    @Override
    @Transactional
    public StaffAttendanceResponseDTO updateAttendance(UUID recordUuid, StaffAttendanceRequestDTO req, Long performedBy) {

        StaffDailyAttendance e = repo.findByUuid(recordUuid)
                .orElseThrow(() -> new AttendanceRecordNotFoundException("Record not found: " + recordUuid));

        Long requestedStaffId = resolveStaffId(req);

        if (!e.getStaffId().equals(requestedStaffId) ||
                !e.getAttendanceDate().equals(req.getAttendanceDate())) {
            throw new AttendanceProcessingException("Cannot change staffId or attendanceDate");
        }

        AttendanceType at;
        if (req.getSource() != AttendanceSource.MANUAL) {
            // Block if on leave
            boolean isOnLeave = leaveApplicationRepository.existsOverlapping(
                    e.getStaffId(),
                    req.getAttendanceDate(),
                    req.getAttendanceDate(),
                    List.of(LeaveApplicationStatus.APPROVED)
            );
            if (isOnLeave) {
                throw new AttendanceProcessingException("Update blocked. You are on an approved leave today.");
            }
        }

        at = resolveAttendanceType(e.getStaffId(), req.getAttendanceDate(), req.getTimeIn(), req.getAttendanceShortCode());

        validateTimes(req.getTimeIn(), req.getTimeOut());
        attendanceEditWindowService.enforceForAttendanceDate(e.getAttendanceDate());

        e.setAttendanceType(at);
        e.setTimeIn(req.getTimeIn());
        e.setTimeOut(req.getTimeOut());
        e.setTotalHours(req.getTotalHours());
        e.setSource(req.getSource());
        e.setNotes(req.getNotes());
        e.setLatitude(req.getLatitude());
        e.setLongitude(req.getLongitude());
        e.setGeoVerified(geoFenceValidator.validateAndResolveGeoVerified(req, performedBy, e.getStaffId()));
        applyEarlyClockOutFlag(e.getStaffId(), req.getAttendanceDate(), req.getTimeOut(), e);

        return toDto(repo.save(e));
    }

    /* -------------------------------------------------------------
     * DELETE
     * ------------------------------------------------------------- */
    @Override
    @Transactional
    public void deleteAttendance(UUID recordUuid, Long performedBy) {
        StaffDailyAttendance e = repo.findByUuid(recordUuid)
                .orElseThrow(() -> new AttendanceRecordNotFoundException("Record not found: " + recordUuid));
        attendanceEditWindowService.enforceForAttendanceDate(e.getAttendanceDate());
        repo.delete(e);
    }

    @Override
    public AttendanceCompletionDTO getAttendanceCompletion(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate fromDate = yearMonth.atDay(1);
        LocalDate toDate = yearMonth.atEndOfMonth();

        List<LocalDate> daysInMonth = fromDate.datesUntil(toDate.plusDays(1)).toList();

        List<Staff> activeStaff = staffRepository.findAll().stream().filter(Staff::isActive).toList();
        Map<Long, List<StaffShiftMapping>> mappingsByStaff = resolveMappingsByStaffForRange(activeStaff, fromDate, toDate);
        ShiftDefinition defaultShift = shiftDefinitionRepository.findFirstByIsDefaultTrueAndActiveTrueOrderByIdAsc().orElse(null);

        long totalExpectedRecords = 0L;
        Map<Long, List<LocalDate>> expectedDatesByStaff = new HashMap<>();
        for (Staff staff : activeStaff) {
            List<LocalDate> expectedDates = daysInMonth.stream()
                    .filter(day -> !isNonWorkingStaffDay(day))
                    .filter(day -> isStaffScheduledOnDate(staff.getId(), day, mappingsByStaff, defaultShift))
                    .toList();
            expectedDatesByStaff.put(staff.getId(), expectedDates);
            totalExpectedRecords += expectedDates.size();
        }

        List<StaffDailyAttendance> monthlyRecords = repo.findAllByDateRange(fromDate, toDate).stream()
                .filter(r -> {
                    List<LocalDate> expectedDates = expectedDatesByStaff.getOrDefault(r.getStaffId(), Collections.emptyList());
                    return expectedDates.contains(r.getAttendanceDate());
                })
                .toList();

        Map<Long, Set<LocalDate>> markedDatesByStaff = new HashMap<>();
        for (StaffDailyAttendance record : monthlyRecords) {
            markedDatesByStaff.computeIfAbsent(record.getStaffId(), ignored -> new HashSet<>()).add(record.getAttendanceDate());
        }

        List<AttendanceCompletionDTO.UnmarkedStaffAttendanceDTO> unmarked = new ArrayList<>();
        for (Staff staff : activeStaff) {
            Set<LocalDate> marked = markedDatesByStaff.getOrDefault(staff.getId(), Collections.emptySet());
            List<LocalDate> expectedDates = expectedDatesByStaff.getOrDefault(staff.getId(), Collections.emptyList());
            List<LocalDate> missing = expectedDates.stream().filter(d -> !marked.contains(d)).toList();
            if (!missing.isEmpty()) {
                String fullName = Optional.ofNullable(staff.getUserProfile())
                        .map(p -> Stream.of(p.getFirstName(), p.getLastName()).filter(Objects::nonNull).collect(Collectors.joining(" ")).trim())
                        .orElse("");
                unmarked.add(AttendanceCompletionDTO.UnmarkedStaffAttendanceDTO.builder()
                        .staffUuid(staff.getUuid() != null ? staff.getUuid().toString() : null)
                        .staffName(fullName)
                        .employeeId(staff.getEmployeeId())
                        .missingDates(missing)
                        .build());
            }
        }

        long totalActualRecords = monthlyRecords.stream().map(r -> r.getStaffId() + "|" + r.getAttendanceDate()).distinct().count();
        double completion = totalExpectedRecords == 0 ? 100.0 : (totalActualRecords * 100.0) / totalExpectedRecords;

        return AttendanceCompletionDTO.builder()
                .month(month)
                .year(year)
                .totalActiveStaff(activeStaff.size())
                .totalWorkingDays(daysInMonth.size())
                .totalExpectedRecords(totalExpectedRecords)
                .totalActualRecords(totalActualRecords)
                .completionPercentage(Math.round(completion * 100.0) / 100.0)
                .isComplete(totalExpectedRecords == totalActualRecords)
                .unmarkedStaff(unmarked)
                .build();
    }

    @Override
    public List<StaffSummaryDTO> getUnmarkedStaff(LocalDate date, Optional<StaffCategory> category) {
        if (isNonWorkingStaffDay(date)) {
            return List.of();
        }

        List<Staff> activeStaff = staffRepository.findAll().stream()
                .filter(Staff::isActive)
                .filter(s -> category.map(c -> c == s.getCategory()).orElse(true))
                .toList();
        List<Staff> expectedStaff = resolveExpectedStaffByDate(activeStaff, date);
        Set<Long> markedStaffIds = new HashSet<>(repo.findDistinctStaffIdsByDate(date));

        return expectedStaff.stream()
                .filter(staff -> !markedStaffIds.contains(staff.getId()))
                .map(this::toStaffSummary)
                .toList();
    }

    /* -------------------------------------------------------------
     * HELPERS
     * ------------------------------------------------------------- */
    private AttendanceType resolveAttendanceType(Long staffId, LocalDate attendanceDate, LocalTime timeIn, String requestedShortCode) {
        if (timeIn != null) {
            return calculateAttendanceTypeBasedOnShift(staffId, attendanceDate, timeIn);
        }
        return attendanceTypeRepo.findByShortCodeIgnoreCase(requestedShortCode)
                .orElseThrow(() -> new AttendanceProcessingException("Invalid attendance short code: " + requestedShortCode));
    }

    private AttendanceType calculateAttendanceTypeBasedOnShift(Long staffId, LocalDate date, LocalTime timeIn) {
        ShiftDefinition shift = staffShiftMappingRepository.findCurrentMappingsByStaffId(staffId, date).stream()
                .map(StaffShiftMapping::getShift)
                .findFirst()
                .orElseGet(() -> shiftDefinitionRepository.findFirstByIsDefaultTrueAndActiveTrueOrderByIdAsc().orElse(null));

        if (shift == null) {
            return attendanceTypeRepo.findByShortCodeIgnoreCase("P")
                    .orElseThrow(() -> new AttendanceProcessingException("Missing AttendanceType P"));
        }

        LocalTime startTime = shift.getStartTime();
        LocalTime endTime = shift.getEndTime();
        int grace = Optional.ofNullable(shift.getGraceMinutes()).orElse(0);

        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        if (totalMinutes < 0) totalMinutes += 1440; 
        
        long elapsed = Duration.between(startTime, timeIn).toMinutes();
        if (elapsed < -720) elapsed += 1440; // If punch in before midnight for night shift
        
        if (elapsed > (totalMinutes / 2)) {
            throw new AttendanceProcessingException("Check-in blocked. You have exceeded the halfway point of your shift and are marked absent. Please contact your Administrator for assistance.");
        } else if (elapsed > grace) {
            return attendanceTypeRepo.findByShortCodeIgnoreCase("L")
                .orElseGet(() -> attendanceTypeRepo.findByShortCodeIgnoreCase("P").orElseThrow(() -> new AttendanceProcessingException("Missing AttendanceType P")));
        } else {
            return attendanceTypeRepo.findByShortCodeIgnoreCase("P").orElseThrow(() -> new AttendanceProcessingException("Missing AttendanceType P"));
        }
    }

    private void validateTimes(LocalTime in, LocalTime out) {
        if (in != null && out != null && in.isAfter(out)) {
            throw new AttendanceProcessingException("timeIn cannot be after timeOut");
        }
    }

    private void applyEarlyClockOutFlag(Long staffId, LocalDate attendanceDate, LocalTime timeOut, StaffDailyAttendance target) {
        target.setEarlyLeave(false);
        target.setEarlyOutMinutes(null);

        if (staffId == null || attendanceDate == null || timeOut == null) {
            return;
        }

        LocalTime shiftEndTime = staffShiftMappingRepository.findCurrentMappingsByStaffId(staffId, attendanceDate).stream()
                .map(StaffShiftMapping::getShift)
                .filter(Objects::nonNull)
                .map(ShiftDefinition::getEndTime)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (shiftEndTime == null) {
            return;
        }

        if (timeOut.isBefore(shiftEndTime)) {
            target.setEarlyLeave(true);
            target.setEarlyOutMinutes((int) Duration.between(timeOut, shiftEndTime).toMinutes());
        }
    }

    /**
     * Convert entity → DTO using your EXACT DTO constructor order.
     */
    private StaffAttendanceResponseDTO toDto(StaffDailyAttendance e) {

        String attendanceMark = null;
        String shortCode = null;
        String colorCode = null;

        if (e.getAttendanceType() != null) {
            attendanceMark  = e.getAttendanceType().getTypeName();
            shortCode       = e.getAttendanceType().getShortCode();
            colorCode       = e.getAttendanceType().getColorCode();
        }

        String recordUuid = e.getUuid() == null ? null : e.getUuid().toString();
        String staffUuid = null;
        String staffName = null;
        String jobTitle = null;

        if (e.getStaffId() != null) {
            Optional<Staff> staffOpt = staffRepository.findById(e.getStaffId());
            if (staffOpt.isPresent()) {
                Staff staff = staffOpt.get();
                staffUuid = staff.getUuid() == null ? null : staff.getUuid().toString();
                jobTitle = staff.getJobTitle();
                if (staff.getUserProfile() != null) {
                    String fn = staff.getUserProfile().getFirstName();
                    String ln = staff.getUserProfile().getLastName();
                    staffName = java.util.stream.Stream.of(fn, ln)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(" "))
                            .trim();
                    if (staffName.isEmpty()) staffName = null;
                }
            }
        }

        return new StaffAttendanceResponseDTO(
                recordUuid,
                staffUuid,
                staffName,
                jobTitle,
                e.getAttendanceDate(),
                attendanceMark,
                shortCode,
                colorCode,
                e.getTimeIn(),
                e.getTimeOut(),
                e.getTotalHours(),
                null, // shiftStartTime
                null, // shiftEndTime
                null, // shiftGraceMinutes
                e.getSource(),
                e.getNotes(),
                e.getLatitude(),
                e.getLongitude(),
                e.getGeoVerified(),
                e.getEarlyLeave(),
                e.getEarlyOutMinutes()
        );
    }

    private StaffSummaryDTO toStaffSummary(Staff staff) {
        UserProfile profile = staff.getUserProfile();
        return StaffSummaryDTO.builder()
                .staffId(staff.getId())
                .uuid(staff.getUuid() != null ? staff.getUuid().toString() : null)
                .employeeId(staff.getEmployeeId())
                .firstName(profile != null ? profile.getFirstName() : null)
                .middleName(profile != null ? profile.getMiddleName() : null)
                .lastName(profile != null ? profile.getLastName() : null)
                .email(profile != null && profile.getUser() != null ? profile.getUser().getEmail() : null)
                .username(profile != null && profile.getUser() != null ? profile.getUser().getUsername() : null)
                .profileUrl(profile != null ? profile.getProfileUrl() : null)
                .dateOfBirth(profile != null ? profile.getDateOfBirth() : null)
                .gender(profile != null && profile.getGender() != null ? profile.getGender().name() : null)
                .jobTitle(staff.getJobTitle())
                .department(staff.getDepartment() != null ? staff.getDepartment().name() : null)
                .staffType(staff.getStaffType())
                .category(staff.getCategory())
                .designationCode(staff.getDesignation() != null ? staff.getDesignation().getDesignationCode() : null)
                .designationName(staff.getDesignation() != null ? staff.getDesignation().getDesignationName() : null)
                .hireDate(staff.getHireDate())
                .officeLocation(staff.getOfficeLocation())
                .active(staff.isActive())
                .build();
    }

    private Long resolveStaffId(StaffAttendanceRequestDTO req) {
        if (req.getStaffUuid() != null) {
            return resolveStaffIdFromUuid(req.getStaffUuid());
        }
        throw new AttendanceProcessingException("staffUuid is required.");
    }

    private Long resolveStaffIdFromUuid(UUID staffUuid) {
        return staffRepository.findByUuid(staffUuid)
                .map(s -> s.getId())
                .orElseThrow(() -> new AttendanceProcessingException("Staff not found for uuid: " + staffUuid));
    }

    private List<Staff> resolveExpectedStaffByDate(List<Staff> activeStaff, LocalDate date) {
        if (activeStaff.isEmpty()) {
            return List.of();
        }

        ShiftDefinition defaultShift = shiftDefinitionRepository.findFirstByIsDefaultTrueAndActiveTrueOrderByIdAsc().orElse(null);
        Map<Long, ShiftDefinition> activeShiftByStaff = resolveActiveShiftByStaff(activeStaff, date);

        return activeStaff.stream()
                .filter(staff -> {
                    ShiftDefinition shift = activeShiftByStaff.getOrDefault(staff.getId(), defaultShift);
                    return isShiftApplicableForDay(shift, date.getDayOfWeek().getValue());
                })
                .toList();
    }

    private Map<Long, ShiftDefinition> resolveActiveShiftByStaff(List<Staff> staffList, LocalDate date) {
        List<Long> staffIds = staffList.stream().map(Staff::getId).toList();
        if (staffIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ShiftDefinition> shiftByStaff = new HashMap<>();
        List<StaffShiftMapping> mappings = staffShiftMappingRepository.findCurrentMappingsByStaffIds(staffIds, date);
        for (StaffShiftMapping mapping : mappings) {
            Long staffId = mapping.getStaff().getId();
            shiftByStaff.putIfAbsent(staffId, mapping.getShift());
        }
        return shiftByStaff;
    }

    private Map<Long, List<StaffShiftMapping>> resolveMappingsByStaffForRange(List<Staff> staffList, LocalDate fromDate, LocalDate toDate) {
        List<Long> staffIds = staffList.stream().map(Staff::getId).toList();
        if (staffIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<StaffShiftMapping> mappings = staffShiftMappingRepository.findMappingsOverlappingDateRange(staffIds, fromDate, toDate);
        Map<Long, List<StaffShiftMapping>> mappingsByStaff = new HashMap<>();
        for (StaffShiftMapping mapping : mappings) {
            mappingsByStaff.computeIfAbsent(mapping.getStaff().getId(), ignored -> new ArrayList<>()).add(mapping);
        }
        return mappingsByStaff;
    }

    private boolean isStaffScheduledOnDate(Long staffId, LocalDate date, Map<Long, List<StaffShiftMapping>> mappingsByStaff, ShiftDefinition defaultShift) {
        List<StaffShiftMapping> mappings = mappingsByStaff.getOrDefault(staffId, Collections.emptyList());
        for (StaffShiftMapping mapping : mappings) {
            LocalDate effectiveFrom = mapping.getEffectiveFrom();
            LocalDate effectiveTo = mapping.getEffectiveTo();
            boolean isActiveOnDate = !effectiveFrom.isAfter(date) && (effectiveTo == null || !effectiveTo.isBefore(date));
            if (isActiveOnDate) {
                return isShiftApplicableForDay(mapping.getShift(), date.getDayOfWeek().getValue());
            }
        }
        return isShiftApplicableForDay(defaultShift, date.getDayOfWeek().getValue());
    }

    private boolean isShiftApplicableForDay(ShiftDefinition shift, int dayOfWeek) {
        if (shift == null) {
            return true;
        }

        String rawDays = shift.getApplicableDays();
        if (rawDays == null || rawDays.isBlank()) {
            return true;
        }

        return Arrays.stream(rawDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::safeParseDay)
                .filter(Objects::nonNull)
                .anyMatch(day -> day == dayOfWeek);
    }

    private Integer safeParseDay(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid applicable day value '{}' while evaluating shift applicability", raw);
            return null;
        }
    }

    private boolean isNonWorkingStaffDay(LocalDate date) {
        return academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(date, NON_WORKING_DAY_TYPES);
    }
}
