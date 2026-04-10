package com.project.edusync.ams.model.service.implementation;

import com.project.edusync.ams.model.dto.request.StaffAttendanceRequestDTO;
import com.project.edusync.ams.model.dto.response.StaffDailyStatsResponseDTO;
import com.project.edusync.ams.model.dto.response.StaffAttendanceResponseDTO;
import com.project.edusync.ams.model.entity.AttendanceType;
import com.project.edusync.ams.model.entity.ShiftDefinition;
import com.project.edusync.ams.model.entity.StaffDailyAttendance;
import com.project.edusync.ams.model.entity.StaffShiftMapping;
import com.project.edusync.ams.model.enums.AttendanceSource;
import com.project.edusync.ams.model.repository.AttendanceTypeRepository;
import com.project.edusync.ams.model.repository.ShiftDefinitionRepository;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.ams.model.repository.StaffShiftMappingRepository;
import com.project.edusync.ams.model.service.AttendanceEditWindowService;
import com.project.edusync.ams.model.service.GeoFenceValidator;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAttendanceServiceImplTest {

    @Mock
    private StaffDailyAttendanceRepository repo;
    @Mock
    private AttendanceTypeRepository attendanceTypeRepo;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private GeoFenceValidator geoFenceValidator;
    @Mock
    private AttendanceEditWindowService attendanceEditWindowService;
    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;
    @Mock
    private AcademicCalendarEventRepository academicCalendarEventRepository;
    @Mock
    private StaffShiftMappingRepository staffShiftMappingRepository;
    @Mock
    private ShiftDefinitionRepository shiftDefinitionRepository;

    @InjectMocks
    private StaffAttendanceServiceImpl service;

    @Test
    void createAttendanceManualSourceRecalculatesLateFromMappedShift() {
        UUID staffUuid = UUID.randomUUID();
        LocalDate attendanceDate = LocalDate.now();

        StaffAttendanceRequestDTO request = new StaffAttendanceRequestDTO(
                staffUuid,
                attendanceDate,
                "P",
                LocalTime.of(9, 11),
                null,
                null,
                AttendanceSource.MANUAL,
                null,
                null,
                null
        );

        Staff staff = new Staff();
        staff.setId(10L);
        staff.setUuid(staffUuid);

        ShiftDefinition shift = buildShift(9, 0, 17, 0, 10);
        StaffShiftMapping mapping = new StaffShiftMapping();
        mapping.setStaff(staff);
        mapping.setShift(shift);

        AttendanceType late = buildType("Late", "L");

        when(staffRepository.findByUuid(staffUuid)).thenReturn(Optional.of(staff));
        when(academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(eq(attendanceDate), any(Set.class))).thenReturn(false);
        when(staffShiftMappingRepository.findCurrentMappingsByStaffId(10L, attendanceDate)).thenReturn(List.of(mapping));
        when(attendanceTypeRepo.findByShortCodeIgnoreCase("L")).thenReturn(Optional.of(late));
        when(repo.findByStaffIdAndAttendanceDate(10L, attendanceDate)).thenReturn(Optional.empty());
        when(geoFenceValidator.validateAndResolveGeoVerified(eq(request), eq(99L), eq(10L))).thenReturn(false);
        when(repo.save(any(StaffDailyAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffAttendanceResponseDTO response = service.createAttendance(request, 99L);

        assertEquals("L", response.getShortCode());
    }

    @Test
    void createAttendanceFallsBackToDefaultShiftWhenNoStaffMapping() {
        UUID staffUuid = UUID.randomUUID();
        LocalDate attendanceDate = LocalDate.now();

        StaffAttendanceRequestDTO request = new StaffAttendanceRequestDTO(
                staffUuid,
                attendanceDate,
                "P",
                LocalTime.of(9, 11),
                null,
                null,
                AttendanceSource.BIOMETRIC,
                null,
                null,
                null
        );

        Staff staff = new Staff();
        staff.setId(10L);
        staff.setUuid(staffUuid);

        ShiftDefinition defaultShift = buildShift(9, 0, 17, 0, 10);
        AttendanceType late = buildType("Late", "L");

        when(staffRepository.findByUuid(staffUuid)).thenReturn(Optional.of(staff));
        when(academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(eq(attendanceDate), any(Set.class))).thenReturn(false);
        when(leaveApplicationRepository.existsOverlapping(eq(10L), eq(attendanceDate), eq(attendanceDate), any())).thenReturn(false);
        when(staffShiftMappingRepository.findCurrentMappingsByStaffId(10L, attendanceDate)).thenReturn(List.of());
        when(shiftDefinitionRepository.findFirstByIsDefaultTrueAndActiveTrueOrderByIdAsc()).thenReturn(Optional.of(defaultShift));
        when(attendanceTypeRepo.findByShortCodeIgnoreCase("L")).thenReturn(Optional.of(late));
        when(repo.findByStaffIdAndAttendanceDate(10L, attendanceDate)).thenReturn(Optional.empty());
        when(geoFenceValidator.validateAndResolveGeoVerified(eq(request), eq(99L), eq(10L))).thenReturn(false);
        when(repo.save(any(StaffDailyAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffAttendanceResponseDTO response = service.createAttendance(request, 99L);

        assertEquals("L", response.getShortCode());
        verify(shiftDefinitionRepository).findFirstByIsDefaultTrueAndActiveTrueOrderByIdAsc();
    }

    @Test
    void createAttendanceSetsGeoVerifiedTrueWhenCoordinatesAreInsideRadius() {
        UUID staffUuid = UUID.randomUUID();
        LocalDate attendanceDate = LocalDate.now();

        StaffAttendanceRequestDTO request = new StaffAttendanceRequestDTO(
                staffUuid,
                attendanceDate,
                "P",
                null,
                null,
                null,
                AttendanceSource.WEB,
                null,
                28.6139,
                77.2090
        );

        Staff staff = new Staff();
        staff.setId(10L);
        staff.setUuid(staffUuid);

        AttendanceType present = buildType("Present", "P");

        when(staffRepository.findByUuid(staffUuid)).thenReturn(Optional.of(staff));
        when(leaveApplicationRepository.existsOverlapping(eq(10L), eq(attendanceDate), eq(attendanceDate), any())).thenReturn(false);
        when(attendanceTypeRepo.findByShortCodeIgnoreCase("P")).thenReturn(Optional.of(present));
        when(repo.findByStaffIdAndAttendanceDate(10L, attendanceDate)).thenReturn(Optional.empty());
        when(geoFenceValidator.verifyByCoordinatesIfPresent(eq(28.6139), eq(77.2090))).thenReturn(true);
        when(repo.save(any(StaffDailyAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffAttendanceResponseDTO response = service.createAttendance(request, 99L);

        assertEquals(true, response.getGeoVerified());
    }

    @Test
    void updateAttendanceRecalculatesStatusForManualEditTimeIn() {
        UUID staffUuid = UUID.randomUUID();
        UUID recordUuid = UUID.randomUUID();
        LocalDate attendanceDate = LocalDate.now();

        StaffAttendanceRequestDTO request = new StaffAttendanceRequestDTO(
                staffUuid,
                attendanceDate,
                "P",
                LocalTime.of(9, 11),
                null,
                null,
                AttendanceSource.MANUAL,
                "admin correction",
                null,
                null
        );

        Staff staff = new Staff();
        staff.setId(10L);
        staff.setUuid(staffUuid);

        ShiftDefinition shift = buildShift(9, 0, 17, 0, 10);
        StaffShiftMapping mapping = new StaffShiftMapping();
        mapping.setStaff(staff);
        mapping.setShift(shift);

        AttendanceType present = buildType("Present", "P");
        AttendanceType late = buildType("Late", "L");

        StaffDailyAttendance existing = new StaffDailyAttendance();
        existing.setStaffId(10L);
        existing.setAttendanceDate(attendanceDate);
        existing.setAttendanceType(present);
        existing.setSource(AttendanceSource.MANUAL);

        when(repo.findByUuid(recordUuid)).thenReturn(Optional.of(existing));
        when(staffRepository.findByUuid(staffUuid)).thenReturn(Optional.of(staff));
        when(staffShiftMappingRepository.findCurrentMappingsByStaffId(10L, attendanceDate)).thenReturn(List.of(mapping));
        when(attendanceTypeRepo.findByShortCodeIgnoreCase("L")).thenReturn(Optional.of(late));
        when(geoFenceValidator.validateAndResolveGeoVerified(eq(request), eq(99L), eq(10L))).thenReturn(false);
        when(repo.save(any(StaffDailyAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffAttendanceResponseDTO response = service.updateAttendance(recordUuid, request, 99L);

        assertEquals("L", response.getShortCode());
    }

    @Test
    void updateAttendanceFlagsEarlyClockOutWithoutChangingAttendanceType() {
        UUID staffUuid = UUID.randomUUID();
        UUID recordUuid = UUID.randomUUID();
        LocalDate attendanceDate = LocalDate.now();

        StaffAttendanceRequestDTO request = new StaffAttendanceRequestDTO(
                staffUuid,
                attendanceDate,
                "P",
                LocalTime.of(9, 0),
                LocalTime.of(16, 15),
                null,
                AttendanceSource.MANUAL,
                "left early",
                null,
                null
        );

        Staff staff = new Staff();
        staff.setId(10L);
        staff.setUuid(staffUuid);

        ShiftDefinition shift = buildShift(9, 0, 17, 0, 10);
        StaffShiftMapping mapping = new StaffShiftMapping();
        mapping.setStaff(staff);
        mapping.setShift(shift);

        AttendanceType present = buildType("Present", "P");

        StaffDailyAttendance existing = new StaffDailyAttendance();
        existing.setStaffId(10L);
        existing.setAttendanceDate(attendanceDate);
        existing.setAttendanceType(present);

        when(repo.findByUuid(recordUuid)).thenReturn(Optional.of(existing));
        when(staffRepository.findByUuid(staffUuid)).thenReturn(Optional.of(staff));
        when(attendanceTypeRepo.findByShortCodeIgnoreCase("P")).thenReturn(Optional.of(present));
        when(staffShiftMappingRepository.findCurrentMappingsByStaffId(10L, attendanceDate)).thenReturn(List.of(mapping));
        when(geoFenceValidator.validateAndResolveGeoVerified(eq(request), eq(99L), eq(10L))).thenReturn(false);
        when(repo.save(any(StaffDailyAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffAttendanceResponseDTO response = service.updateAttendance(recordUuid, request, 99L);

        assertEquals("P", response.getShortCode());
        assertEquals(true, response.getEarlyLeave());
        assertEquals(45, response.getEarlyOutMinutes());
    }

    @Test
    void createAttendanceRejectsSelfCheckInOnHoliday() {
        UUID staffUuid = UUID.randomUUID();
        LocalDate attendanceDate = LocalDate.of(2026, 4, 10);

        Staff staff = new Staff();
        staff.setId(10L);
        staff.setUuid(staffUuid);

        StaffAttendanceRequestDTO request = new StaffAttendanceRequestDTO(
                staffUuid,
                attendanceDate,
                "P",
                LocalTime.of(9, 0),
                null,
                null,
                AttendanceSource.BIOMETRIC,
                null,
                null,
                null
        );

        when(staffRepository.findByUuid(staffUuid)).thenReturn(Optional.of(staff));
        when(academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(eq(attendanceDate), any(Set.class))).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createAttendance(request, 99L));

        assertEquals("Cannot clock in on an official holiday.", ex.getMessage());
        verify(repo, never()).save(any(StaffDailyAttendance.class));
    }

    @Test
    void bulkCreateRejectsHolidayDate() {
        UUID staffUuid = UUID.randomUUID();
        LocalDate attendanceDate = LocalDate.of(2026, 4, 10);

        StaffAttendanceRequestDTO request = new StaffAttendanceRequestDTO(
                staffUuid,
                attendanceDate,
                "P",
                LocalTime.of(9, 0),
                null,
                null,
                AttendanceSource.MANUAL,
                null,
                null,
                null
        );

        when(academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(eq(attendanceDate), any(Set.class))).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.bulkCreate(List.of(request), 99L));
        verify(repo, never()).save(any(StaffDailyAttendance.class));
    }

    @Test
    void getDailyStatsBuildsAggregatesAndUnmarkedCount() {
        LocalDate date = LocalDate.of(2026, 4, 10);

        List<Staff> activeStaff = IntStream.rangeClosed(1, 150)
                .mapToObj(i -> {
                    Staff staff = new Staff();
                    staff.setId((long) i);
                    staff.setActive(true);
                    return staff;
                })
                .toList();

        ShiftDefinition defaultShift = buildShift(9, 0, 17, 0, 10);
        defaultShift.setApplicableDays("1,2,3,4,5,6,7");

        when(academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(eq(date), any(Set.class))).thenReturn(false);
        when(repo.countByDateGroupedByShortCode(date)).thenReturn(List.of(
                projection("P", 85L),
                projection("A", 5L),
                projection("L", 25L),
                projection("LV", 5L)
        ));
        when(staffRepository.findAll()).thenReturn(activeStaff);
        when(staffShiftMappingRepository.findCurrentMappingsByStaffIds(any(), eq(date))).thenReturn(List.of());
        when(shiftDefinitionRepository.findFirstByIsDefaultTrueAndActiveTrueOrderByIdAsc()).thenReturn(Optional.of(defaultShift));

        StaffDailyStatsResponseDTO stats = service.getDailyStats(Optional.of(date));

        assertEquals(date, stats.date());
        assertEquals(120L, stats.totalMarked());
        assertEquals(85L, stats.present());
        assertEquals(5L, stats.absent());
        assertEquals(25L, stats.late());
        assertEquals(5L, stats.onLeave());
        assertEquals(30L, stats.unmarkedCount());
    }

    @Test
    void listAttendancesReturnsEmptyPageWhenSearchHasNoMatchingStaff() {
        Pageable pageable = PageRequest.of(0, 20);
        when(staffRepository.findStaffIdsBySearch("Aamir")).thenReturn(List.of());

        Page<StaffAttendanceResponseDTO> page = service.listAttendances(
                pageable,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("Aamir")
        );

        assertEquals(0L, page.getTotalElements());
        verify(repo, never()).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void listAttendancesSupportsStatusFilterArgument() {
        Pageable pageable = PageRequest.of(0, 20);

        AttendanceType present = buildType("Present", "P");
        StaffDailyAttendance record = new StaffDailyAttendance();
        record.setStaffId(10L);
        record.setAttendanceDate(LocalDate.of(2026, 4, 10));
        record.setAttendanceType(present);

        Staff staff = new Staff();
        staff.setId(10L);

        when(repo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(record), pageable, 1));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(staff));

        Page<StaffAttendanceResponseDTO> page = service.listAttendances(
                pageable,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("P"),
                Optional.empty()
        );

        assertEquals(1L, page.getTotalElements());
        assertEquals("P", page.getContent().get(0).getShortCode());
        verify(repo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getUnmarkedStaffExcludesShiftInapplicableDay() {
        LocalDate sunday = LocalDate.of(2026, 4, 12);

        Staff weekdayStaff = new Staff();
        weekdayStaff.setId(1L);
        weekdayStaff.setActive(true);

        Staff sundayStaff = new Staff();
        sundayStaff.setId(2L);
        sundayStaff.setActive(true);

        ShiftDefinition weekdayShift = buildShift(9, 0, 17, 0, 10);
        weekdayShift.setApplicableDays("1,2,3,4,5");

        ShiftDefinition weekendShift = buildShift(9, 0, 17, 0, 10);
        weekendShift.setApplicableDays("7");

        StaffShiftMapping weekdayMapping = buildMapping(weekdayStaff, weekdayShift, sunday.minusDays(10), null);
        StaffShiftMapping weekendMapping = buildMapping(sundayStaff, weekendShift, sunday.minusDays(10), null);

        when(academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(eq(sunday), any(Set.class))).thenReturn(false);
        when(staffRepository.findAll()).thenReturn(List.of(weekdayStaff, sundayStaff));
        when(staffShiftMappingRepository.findCurrentMappingsByStaffIds(List.of(1L, 2L), sunday)).thenReturn(List.of(weekdayMapping, weekendMapping));
        when(shiftDefinitionRepository.findFirstByIsDefaultTrueAndActiveTrueOrderByIdAsc()).thenReturn(Optional.empty());
        when(repo.findDistinctStaffIdsByDate(sunday)).thenReturn(List.of());

        List<com.project.edusync.uis.model.dto.admin.StaffSummaryDTO> unmarked = service.getUnmarkedStaff(sunday, Optional.empty());

        assertEquals(1, unmarked.size());
        assertEquals(2L, unmarked.get(0).getStaffId());
    }

    @Test
    void getUnmarkedStaffReturnsEmptyOnGlobalHoliday() {
        LocalDate date = LocalDate.of(2026, 4, 12);
        when(academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(eq(date), any(Set.class))).thenReturn(true);

        List<com.project.edusync.uis.model.dto.admin.StaffSummaryDTO> unmarked = service.getUnmarkedStaff(date, Optional.empty());

        assertEquals(0, unmarked.size());
        verify(staffRepository, never()).findAll();
        verify(repo, never()).findDistinctStaffIdsByDate(any(LocalDate.class));
    }

    @Test
    void getDailyStatsUsesOnlyShiftApplicableStaffForUnmarkedCount() {
        LocalDate sunday = LocalDate.of(2026, 4, 12);

        Staff weekdayStaff = new Staff();
        weekdayStaff.setId(1L);
        weekdayStaff.setActive(true);

        Staff sundayStaff = new Staff();
        sundayStaff.setId(2L);
        sundayStaff.setActive(true);

        ShiftDefinition weekdayShift = buildShift(9, 0, 17, 0, 10);
        weekdayShift.setApplicableDays("1,2,3,4,5");

        ShiftDefinition weekendShift = buildShift(9, 0, 17, 0, 10);
        weekendShift.setApplicableDays("7");

        StaffShiftMapping weekdayMapping = buildMapping(weekdayStaff, weekdayShift, sunday.minusDays(10), null);
        StaffShiftMapping weekendMapping = buildMapping(sundayStaff, weekendShift, sunday.minusDays(10), null);

        when(academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStaffTrueAndIsActiveTrue(eq(sunday), any(Set.class))).thenReturn(false);
        when(repo.countByDateGroupedByShortCode(sunday)).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of(weekdayStaff, sundayStaff));
        when(staffShiftMappingRepository.findCurrentMappingsByStaffIds(List.of(1L, 2L), sunday)).thenReturn(List.of(weekdayMapping, weekendMapping));
        when(shiftDefinitionRepository.findFirstByIsDefaultTrueAndActiveTrueOrderByIdAsc()).thenReturn(Optional.empty());

        StaffDailyStatsResponseDTO stats = service.getDailyStats(Optional.of(sunday));

        assertEquals(1L, stats.unmarkedCount());
    }

    private ShiftDefinition buildShift(int startHour, int startMinute, int endHour, int endMinute, int graceMinutes) {
        ShiftDefinition shift = new ShiftDefinition();
        shift.setStartTime(LocalTime.of(startHour, startMinute));
        shift.setEndTime(LocalTime.of(endHour, endMinute));
        shift.setGraceMinutes(graceMinutes);
        return shift;
    }

    private StaffShiftMapping buildMapping(Staff staff, ShiftDefinition shift, LocalDate effectiveFrom, LocalDate effectiveTo) {
        StaffShiftMapping mapping = new StaffShiftMapping();
        mapping.setStaff(staff);
        mapping.setShift(shift);
        mapping.setEffectiveFrom(effectiveFrom);
        mapping.setEffectiveTo(effectiveTo);
        return mapping;
    }

    private AttendanceType buildType(String name, String shortCode) {
        AttendanceType type = new AttendanceType();
        type.setTypeName(name);
        type.setShortCode(shortCode);
        return type;
    }

    private StaffDailyAttendanceRepository.StaffDailyStatusCountProjection projection(String shortCode, Long count) {
        return new StaffDailyAttendanceRepository.StaffDailyStatusCountProjection() {
            @Override
            public String getShortCode() {
                return shortCode;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}

