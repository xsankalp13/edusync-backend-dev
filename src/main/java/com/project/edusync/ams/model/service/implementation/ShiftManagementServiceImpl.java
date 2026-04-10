package com.project.edusync.ams.model.service.implementation;

import com.project.edusync.ams.model.dto.request.BulkStaffShiftMapRequestDTO;
import com.project.edusync.ams.model.dto.request.ShiftCreateDTO;
import com.project.edusync.ams.model.dto.request.StaffShiftMapRequestDTO;
import com.project.edusync.ams.model.dto.response.ShiftMappingResultDTO;
import com.project.edusync.ams.model.dto.response.ShiftResponseDTO;
import com.project.edusync.ams.model.dto.response.StaffShiftMappingResponseDTO;
import com.project.edusync.ams.model.entity.ShiftDefinition;
import com.project.edusync.ams.model.entity.StaffShiftMapping;
import com.project.edusync.ams.model.repository.ShiftDefinitionRepository;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.ams.model.repository.StaffShiftMappingRepository;
import com.project.edusync.ams.model.service.ShiftManagementService;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.repository.PayrollEntryRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShiftManagementServiceImpl implements ShiftManagementService {

    private final ShiftDefinitionRepository shiftDefinitionRepository;
    private final StaffShiftMappingRepository staffShiftMappingRepository;
    private final StaffRepository staffRepository;
    private final StaffDailyAttendanceRepository staffDailyAttendanceRepository;
    private final PayrollEntryRepository payrollEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponseDTO> listShifts() {
        return shiftDefinitionRepository.findByActiveTrueOrderByShiftNameAsc().stream().map(this::toShiftDto).toList();
    }

    @Override
    public ShiftResponseDTO createShift(ShiftCreateDTO request) {
        validateShiftRequest(request, null);
        ShiftDefinition shift = new ShiftDefinition();
        applyShift(shift, request);
        ShiftDefinition saved = shiftDefinitionRepository.save(shift);
        enforceSingleDefault(saved);
        return toShiftDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftResponseDTO getShift(UUID shiftUuid) {
        return toShiftDto(findShift(shiftUuid));
    }

    @Override
    public ShiftResponseDTO updateShift(UUID shiftUuid, ShiftCreateDTO request) {
        ShiftDefinition shift = findShift(shiftUuid);
        if (!Boolean.TRUE.equals(shift.getActive())) {
            throw new EdusyncException("Cannot update an inactive shift", HttpStatus.CONFLICT);
        }

        validateShiftRequest(request, shift.getId());
        applyShift(shift, request);
        ShiftDefinition saved = shiftDefinitionRepository.save(shift);
        enforceSingleDefault(saved);
        return toShiftDto(saved);
    }

    @Override
    public void deleteShift(UUID shiftUuid) {
        ShiftDefinition shift = findShift(shiftUuid);
        if (!Boolean.TRUE.equals(shift.getActive())) {
            return;
        }

        if (staffShiftMappingRepository.existsByShift_IdAndEffectiveToIsNull(shift.getId())) {
            throw new EdusyncException("Cannot delete shift with active staff mappings", HttpStatus.CONFLICT);
        }

        if (Boolean.TRUE.equals(shift.getIsDefault()) && !shiftDefinitionRepository.existsByIsDefaultTrueAndActiveTrueAndIdNot(shift.getId())) {
            throw new EdusyncException("Cannot delete the only default shift", HttpStatus.CONFLICT);
        }

        shift.setActive(false);
        shift.setIsDefault(false);
        shiftDefinitionRepository.save(shift);
    }

    @Override
    public ShiftMappingResultDTO mapSingle(StaffShiftMapRequestDTO request) {
        List<String> errors = new ArrayList<>();
        int success = mapOne(request.getStaffUuid(), request.getShiftUuid(), request.getEffectiveFrom(), errors);
        return ShiftMappingResultDTO.builder().success(success).failed(success == 1 ? 0 : 1).errors(errors).build();
    }

    @Override
    public ShiftMappingResultDTO mapBulk(BulkStaffShiftMapRequestDTO request) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        for (UUID staffUuid : request.getStaffUuids()) {
            success += mapOne(staffUuid, request.getShiftUuid(), request.getEffectiveFrom(), errors);
        }
        return ShiftMappingResultDTO.builder()
                .success(success)
                .failed(request.getStaffUuids().size() - success)
                .errors(errors)
                .build();
    }

    @Override
    public void deleteMapping(UUID mappingUuid) {
        StaffShiftMapping mapping = staffShiftMappingRepository.findByUuid(mappingUuid)
                .orElseThrow(() -> new EdusyncException("Shift mapping not found", HttpStatus.NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDate from = mapping.getEffectiveFrom();
        LocalDate to = mapping.getEffectiveTo() == null ? today : mapping.getEffectiveTo();
        if (to.isBefore(from)) {
            to = from;
        }

        long attendanceCount = staffDailyAttendanceRepository.countByStaffIdAndAttendanceDateBetween(
                mapping.getStaff().getId(), from, to);
        if (attendanceCount > 0) {
            throw new EdusyncException("Cannot delete mapping due to active attendance lock", HttpStatus.CONFLICT);
        }

        int fromYearMonth = toYearMonthValue(from);
        int toYearMonth = toYearMonthValue(to);
        boolean payrollLocked = payrollEntryRepository.existsLockedPayrollForStaffAndPeriod(
                mapping.getStaff().getId(),
                fromYearMonth,
                toYearMonth,
                EnumSet.of(PayrollRunStatus.PROCESSED, PayrollRunStatus.APPROVED, PayrollRunStatus.DISBURSED)
        );
        if (payrollLocked) {
            throw new EdusyncException("Cannot delete mapping due to payroll lock", HttpStatus.CONFLICT);
        }

        if (mapping.getEffectiveTo() == null) {
            LocalDate previousEndDate = mapping.getEffectiveFrom().minusDays(1);
            staffShiftMappingRepository.findTopByStaff_IdAndEffectiveToOrderByEffectiveFromDesc(mapping.getStaff().getId(), previousEndDate)
                    .ifPresent(previous -> {
                        previous.setEffectiveTo(null);
                        staffShiftMappingRepository.save(previous);
                    });
        }

        staffShiftMappingRepository.delete(mapping);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffShiftMappingResponseDTO> listMappings(Pageable pageable, UUID shiftUuid, StaffCategory category) {
        return staffShiftMappingRepository.findCurrentMappings(shiftUuid, category, pageable).map(this::toMappingDto);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffShiftMappingResponseDTO getCurrentMappingByStaff(UUID staffUuid) {
        Staff staff = staffRepository.findByUuid(staffUuid)
                .orElseThrow(() -> new EdusyncException("Staff not found", HttpStatus.NOT_FOUND));
        StaffShiftMapping mapping = staffShiftMappingRepository.findCurrentMappingsByStaffId(staff.getId(), LocalDate.now())
                .stream()
                .findFirst()
                .orElseThrow(() -> new EdusyncException("No active shift mapping found for staff", HttpStatus.NOT_FOUND));
        return toMappingDto(mapping);
    }

    private int mapOne(UUID staffUuid, UUID shiftUuid, LocalDate effectiveFrom, List<String> errors) {
        Staff staff = staffRepository.findByUuid(staffUuid).orElse(null);
        if (staff == null) {
            errors.add("Staff " + staffUuid + " not found");
            return 0;
        }

        ShiftDefinition shift = shiftDefinitionRepository.findByUuid(shiftUuid).orElse(null);
        if (shift == null || !Boolean.TRUE.equals(shift.getActive())) {
            errors.add("Shift " + shiftUuid + " not found");
            return 0;
        }

        if (staffShiftMappingRepository.existsForSameStartDate(staff.getId(), shift.getId(), effectiveFrom)) {
            errors.add("Staff " + staffUuid + " already mapped to this shift");
            return 0;
        }

        staffShiftMappingRepository.findCurrentMappingsByStaffId(staff.getId(), effectiveFrom).stream().findFirst().ifPresent(existing -> {
            existing.setEffectiveTo(effectiveFrom.minusDays(1));
            staffShiftMappingRepository.save(existing);
        });

        StaffShiftMapping mapping = new StaffShiftMapping();
        mapping.setStaff(staff);
        mapping.setShift(shift);
        mapping.setEffectiveFrom(effectiveFrom);
        mapping.setEffectiveTo(null);
        staffShiftMappingRepository.save(mapping);
        return 1;
    }

    private ShiftDefinition findShift(UUID shiftUuid) {
        return shiftDefinitionRepository.findByUuid(shiftUuid)
                .orElseThrow(() -> new EdusyncException("Shift not found", HttpStatus.NOT_FOUND));
    }

    private void validateShiftRequest(ShiftCreateDTO request, Long existingShiftId) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new EdusyncException("endTime must be after startTime", HttpStatus.BAD_REQUEST);
        }

        List<Integer> days = request.getApplicableDays();
        if (days == null || days.isEmpty()) {
            throw new EdusyncException("At least one applicable day is required", HttpStatus.BAD_REQUEST);
        }
        if (days.stream().anyMatch(day -> day == null || day < 1 || day > 7)) {
            throw new EdusyncException("applicableDays must contain values between 1 and 7", HttpStatus.BAD_REQUEST);
        }
        if (days.stream().distinct().count() != days.size()) {
            throw new EdusyncException("applicableDays cannot contain duplicate values", HttpStatus.BAD_REQUEST);
        }

        int graceMinutes = request.getGraceMinutes() == null ? 0 : request.getGraceMinutes();
        if (graceMinutes < 0) {
            throw new EdusyncException("graceMinutes must be greater than or equal to 0", HttpStatus.BAD_REQUEST);
        }

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());
        Long currentId = existingShiftId == null ? -1L : existingShiftId;
        if (!isDefault && !shiftDefinitionRepository.existsByIsDefaultTrueAndActiveTrueAndIdNot(currentId)) {
            throw new EdusyncException("At least one active default shift is required", HttpStatus.CONFLICT);
        }
    }

    private void enforceSingleDefault(ShiftDefinition savedShift) {
        if (!Boolean.TRUE.equals(savedShift.getIsDefault())) {
            return;
        }

        List<ShiftDefinition> otherDefaults = shiftDefinitionRepository.findByIsDefaultTrueAndActiveTrueAndIdNot(savedShift.getId());
        if (otherDefaults.isEmpty()) {
            return;
        }

        otherDefaults.forEach(shift -> shift.setIsDefault(false));
        shiftDefinitionRepository.saveAll(otherDefaults);
    }

    private int toYearMonthValue(LocalDate date) {
        YearMonth ym = YearMonth.from(date);
        return (ym.getYear() * 100) + ym.getMonthValue();
    }

    private void applyShift(ShiftDefinition shift, ShiftCreateDTO request) {
        shift.setShiftName(request.getShiftName());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        shift.setGraceMinutes(request.getGraceMinutes() == null ? 0 : request.getGraceMinutes());
        shift.setApplicableDays((request.getApplicableDays() == null ? List.<Integer>of() : request.getApplicableDays())
                .stream().map(String::valueOf).collect(Collectors.joining(",")));
        shift.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        if (shift.getActive() == null) {
            shift.setActive(true);
        }
    }

    private ShiftResponseDTO toShiftDto(ShiftDefinition shift) {
        List<Integer> days = Arrays.stream((shift.getApplicableDays() == null ? "" : shift.getApplicableDays()).split(","))
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .toList();

        return ShiftResponseDTO.builder()
                .id(shift.getId())
                .uuid(shift.getUuid() != null ? shift.getUuid().toString() : null)
                .shiftName(shift.getShiftName())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .graceMinutes(shift.getGraceMinutes())
                .applicableDays(days)
                .isDefault(shift.getIsDefault())
                .active(shift.getActive())
                .createdAt(shift.getCreatedAt())
                .updatedAt(shift.getUpdatedAt())
                .build();
    }

    private StaffShiftMappingResponseDTO toMappingDto(StaffShiftMapping mapping) {
        Staff staff = mapping.getStaff();
        UserProfile profile = staff.getUserProfile();
        String fullName = (profile == null ? "" :
                java.util.stream.Stream.of(profile.getFirstName(), profile.getLastName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(" "))).trim();

        return StaffShiftMappingResponseDTO.builder()
                .mappingId(mapping.getId())
                .uuid(mapping.getUuid() == null ? null : mapping.getUuid().toString())
                .staffUuid(staff.getUuid() == null ? null : staff.getUuid().toString())
                .staffName(fullName)
                .employeeId(staff.getEmployeeId())
                .staffCategory(staff.getCategory() == null ? null : staff.getCategory().name())
                .shiftUuid(mapping.getShift().getUuid() == null ? null : mapping.getShift().getUuid().toString())
                .shiftName(mapping.getShift().getShiftName())
                .shiftStartTime(mapping.getShift().getStartTime())
                .shiftEndTime(mapping.getShift().getEndTime())
                .effectiveFrom(mapping.getEffectiveFrom())
                .effectiveTo(mapping.getEffectiveTo())
                .build();
    }
}

