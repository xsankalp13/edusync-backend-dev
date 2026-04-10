package com.project.edusync.ams.model.service.implementation;

import com.project.edusync.ams.model.dto.request.StudentAttendanceRequestDTO;
import com.project.edusync.ams.model.dto.response.AttendanceTypeResponseDTO;
import com.project.edusync.ams.model.dto.response.AbsenceDocumentationSummaryResponseDTO;
import com.project.edusync.ams.model.dto.response.StudentAttendanceCompletionDTO;
import com.project.edusync.ams.model.dto.response.StudentAttendanceResponseDTO;
import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.AcademicClassRepository;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.ams.model.entity.AttendanceType;
import com.project.edusync.ams.model.entity.AbsenceDocumentation;
import com.project.edusync.ams.model.entity.StudentDailyAttendance;
import com.project.edusync.ams.model.exception.AttendanceProcessingException;
import com.project.edusync.ams.model.exception.AttendanceRecordNotFoundException;
import com.project.edusync.ams.model.exception.InvalidAttendanceTypeException;
import com.project.edusync.ams.model.repository.AttendanceTypeRepository;
import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.ams.model.repository.AbsenceDocumentationRepository;
import com.project.edusync.ams.model.service.StudentAttendanceService;
import com.project.edusync.ams.model.service.AttendanceEditWindowService;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StudentAttendanceServiceImpl — implements StudentAttendanceService
 * Uses AttendanceType entity relation on StudentDailyAttendance (setAttendanceType / getAttendanceType).
 *
 * Conservative implementation: avoids calling non-existent getters/setters
 * and maps types to match DTO constructors exactly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentAttendanceServiceImpl implements StudentAttendanceService {

    private static final Set<DayType> NON_WORKING_DAY_TYPES = EnumSet.of(DayType.HOLIDAY, DayType.VACATION);

    private final StudentDailyAttendanceRepository studentRepo;
    private final AttendanceTypeRepository attendanceTypeRepository;
    private final AbsenceDocumentationRepository absenceDocumentationRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final AttendanceEditWindowService attendanceEditWindowService;
    private final AcademicClassRepository academicClassRepository;
    private final SectionRepository sectionRepository;
    private final AcademicCalendarEventRepository academicCalendarEventRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"dashboard", "dashboardOverview"}, allEntries = true)
    public List<StudentAttendanceResponseDTO> markAttendanceBatch(List<StudentAttendanceRequestDTO> requests, Long performedByStaffId) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        Optional<LocalDate> blockedDate = requests.stream()
                .map(StudentAttendanceRequestDTO::getAttendanceDate)
                .filter(Objects::nonNull)
                .filter(this::isNonWorkingStudentDay)
                .findFirst();

        if (blockedDate.isPresent()) {
            throw new IllegalArgumentException("Cannot mark student attendance on a non-working day.");
        }

        // Collect unique uppercased short codes
        Set<String> shortCodes = requests.stream()
                .map(r -> Optional.ofNullable(r.getAttendanceShortCode()).orElse("").trim().toUpperCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        // Validate existence of short codes by loading AttendanceType entities
        Map<String, AttendanceType> shortCodeToType = new HashMap<>();
        if (!shortCodes.isEmpty()) {
            for (String sc : shortCodes) {
                attendanceTypeRepository.findByShortCodeIgnoreCase(sc)
                        .ifPresent(t -> shortCodeToType.put(sc, t));
            }
            // report missing
            Set<String> missing = new HashSet<>(shortCodes);
            missing.removeAll(shortCodeToType.keySet());
            if (!missing.isEmpty()) {
                log.warn("Attendance short codes not found: {}", missing);
                throw new InvalidAttendanceTypeException("Attendance short code(s) not found: " + missing);
            }
        }

        List<StudentDailyAttendance> savedEntities = new ArrayList<>(requests.size());

        for (StudentAttendanceRequestDTO req : requests) {
            // Row-level validation
            Long resolvedStudentId = resolveStudentId(req);
            validateAttendanceDateWindow(req.getAttendanceDate());
            String sc = Optional.ofNullable(req.getAttendanceShortCode()).orElse("").trim().toUpperCase();
            if (sc.isEmpty()) {
                throw new InvalidAttendanceTypeException("attendanceShortCode is required (e.g., P, A, L)");
            }

            AttendanceType attendanceType = shortCodeToType.get(sc);
            if (attendanceType == null) {
                // Defensive: try a direct lookup
                attendanceType = attendanceTypeRepository.findByShortCodeIgnoreCase(sc)
                        .orElseThrow(() -> new InvalidAttendanceTypeException("Attendance short code not found: " + sc));
            }

            // Upsert by unique (studentId + date)
            Optional<StudentDailyAttendance> existingOpt = studentRepo.findByStudentIdAndAttendanceDate(resolvedStudentId, req.getAttendanceDate());
            StudentDailyAttendance entity;
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();

                // If same attendance type and same notes, skip update to avoid churn
                AttendanceType currentType = entity.getAttendanceType();
                boolean sameType = currentType != null && Objects.equals(currentType.getId(), attendanceType.getId());
                boolean sameNotes = Objects.equals(entity.getNotes(), req.getNotes());
                if (sameType && sameNotes) {
                    savedEntities.add(entity);
                    continue;
                }
            } else {
                entity = new StudentDailyAttendance();
                entity.setStudentId(resolvedStudentId);
                entity.setAttendanceDate(req.getAttendanceDate());
            }

            // Set relationship to AttendanceType entity
            entity.setAttendanceType(attendanceType);

            // TakenBy staff id - use performedByStaffId if present, else DTO's takenBy
            entity.setTakenByStaffId(Optional.ofNullable(performedByStaffId).orElseGet(() -> resolveTakenByStaffId(req)));
            entity.setNotes(req.getNotes());

            StudentDailyAttendance saved = studentRepo.save(entity);
            savedEntities.add(saved);
        }

        // Map to response DTOs
        return savedEntities.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<StudentAttendanceResponseDTO> listAttendances(Pageable pageable,
                                                              Optional<UUID> studentUuid,
                                                              Optional<UUID> takenByStaffUuid,
                                                              Optional<String> fromDateIso,
                                                              Optional<String> toDateIso,
                                                              Optional<String> attendanceTypeShortCode) {

        Optional<Long> studentId = studentUuid.map(this::resolveStudentIdFromUuid);
        Optional<Long> takenByStaffId = takenByStaffUuid.map(this::resolveStaffIdFromUuid);
        Optional<LocalDate> fromDate = fromDateIso.map(this::parseIsoDate);
        Optional<LocalDate> toDate = toDateIso.map(this::parseIsoDate);

        if (fromDate.isPresent() && toDate.isPresent() && fromDate.get().isAfter(toDate.get())) {
            throw new AttendanceProcessingException("fromDate cannot be after toDate");
        }

        Specification<StudentDailyAttendance> spec = (root, query, cb) -> cb.conjunction();

        if (studentId.isPresent()) {
            Long id = studentId.get();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("studentId"), id));
        }

        if (takenByStaffId.isPresent()) {
            Long id = takenByStaffId.get();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("takenByStaffId"), id));
        }

        if (fromDate.isPresent()) {
            LocalDate date = fromDate.get();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("attendanceDate"), date));
        }

        if (toDate.isPresent()) {
            LocalDate date = toDate.get();
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("attendanceDate"), date));
        }

        if (attendanceTypeShortCode.isPresent()) {
            String sc = attendanceTypeShortCode.get().trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("attendanceType").get("shortCode")), sc));
        }

        Page<StudentDailyAttendance> page = studentRepo.findAll(spec, pageable);
        List<StudentAttendanceResponseDTO> dtoList = page.getContent().stream().map(this::toResponseDto).collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }

    @Override
    public StudentAttendanceResponseDTO getAttendance(UUID recordUuid) {
        StudentDailyAttendance e = studentRepo.findByUuid(recordUuid)
                .orElseThrow(() -> new AttendanceRecordNotFoundException("Attendance record not found with uuid: " + recordUuid));
        return toResponseDto(e);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"dashboard", "dashboardOverview"}, allEntries = true)
    public StudentAttendanceResponseDTO updateAttendance(UUID recordUuid, StudentAttendanceRequestDTO req, Long performedByStaffId) {
        StudentDailyAttendance existing = studentRepo.findByUuid(recordUuid)
                .orElseThrow(() -> new AttendanceRecordNotFoundException("Attendance record not found with uuid: " + recordUuid));

        if (req.getAttendanceDate() != null) {
            validateAttendanceDateWindow(req.getAttendanceDate());
        }

        // Do not allow attendanceDate change
        if (req.getAttendanceDate() != null && !req.getAttendanceDate().equals(existing.getAttendanceDate())) {
            throw new AttendanceProcessingException("attendanceDate cannot be changed for existing record");
        }

        attendanceEditWindowService.enforceForAttendanceDate(existing.getAttendanceDate());

        if (req.getAttendanceShortCode() != null) {
            String sc = req.getAttendanceShortCode().trim().toUpperCase();
            AttendanceType t = attendanceTypeRepository.findByShortCodeIgnoreCase(sc)
                    .orElseThrow(() -> new InvalidAttendanceTypeException("Attendance short code not found: " + sc));
            existing.setAttendanceType(t);
        }

        if (req.getNotes() != null) existing.setNotes(req.getNotes());
        if (performedByStaffId != null) existing.setTakenByStaffId(performedByStaffId);

        StudentDailyAttendance saved = studentRepo.save(existing);
        return toResponseDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"dashboard", "dashboardOverview"}, allEntries = true)
    public void deleteAttendance(UUID recordUuid, Long performedByStaffId) {
        StudentDailyAttendance existing = studentRepo.findByUuid(recordUuid)
                .orElseThrow(() -> new AttendanceRecordNotFoundException("Attendance record not found with uuid: " + recordUuid));

        attendanceEditWindowService.enforceForAttendanceDate(existing.getAttendanceDate());

        // Soft-delete if entity has 'setDeleted' method (some entities in codebase use AuditableEntity)
        try {
            existing.getClass().getDeclaredMethod("setDeleted", Boolean.class);
            existing.getClass().getMethod("setDeleted", Boolean.class).invoke(existing, Boolean.TRUE);
            try {
                existing.getClass().getMethod("setDeletedAt", LocalDateTime.class).invoke(existing, LocalDateTime.now());
            } catch (NoSuchMethodException ignore) {
            }
            studentRepo.save(existing);
        } catch (NoSuchMethodException e) {
            studentRepo.delete(existing);
        } catch (Exception ex) {
            throw new AttendanceProcessingException("Failed to delete attendance record: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public StudentAttendanceCompletionDTO getAttendanceCompletion(UUID classUuid, UUID sectionUuid, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new AttendanceProcessingException("fromDate and toDate are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new AttendanceProcessingException("fromDate cannot be after toDate");
        }

        AcademicClass academicClass = academicClassRepository.findById(classUuid)
                .orElseThrow(() -> new AttendanceProcessingException("Class not found for uuid: " + classUuid));

        List<Long> studentIds;
        if (sectionUuid != null) {
            Section section = sectionRepository.findByUuid(sectionUuid)
                    .orElseThrow(() -> new AttendanceProcessingException("Section not found for uuid: " + sectionUuid));
            studentIds = studentRepository.findBySectionId(section.getId()).stream().map(s -> s.getId()).toList();
        } else {
            studentIds = studentRepository.findBySection_AcademicClass_Id(academicClass.getId()).stream().map(s -> s.getId()).toList();
        }

        if (studentIds.isEmpty()) {
            return StudentAttendanceCompletionDTO.builder()
                    .classUuid(classUuid.toString())
                    .sectionUuid(sectionUuid == null ? null : sectionUuid.toString())
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .totalStudents(0)
                    .datesWithRecords(List.of())
                    .datesWithoutRecords(fromDate.datesUntil(toDate.plusDays(1)).toList())
                    .build();
        }

        List<LocalDate> withRecords = studentRepo.findDistinctDatesWithRecords(studentIds, fromDate, toDate);
        Set<LocalDate> withSet = new HashSet<>(withRecords);
        List<LocalDate> withoutRecords = fromDate.datesUntil(toDate.plusDays(1)).filter(d -> !withSet.contains(d)).toList();

        return StudentAttendanceCompletionDTO.builder()
                .classUuid(classUuid.toString())
                .sectionUuid(sectionUuid == null ? null : sectionUuid.toString())
                .fromDate(fromDate)
                .toDate(toDate)
                .totalStudents(studentIds.size())
                .datesWithRecords(withRecords)
                .datesWithoutRecords(withoutRecords)
                .build();
    }

    /* ------------------ Helper: map entity -> DTO ------------------ */

    private StudentAttendanceResponseDTO toResponseDto(StudentDailyAttendance e) {
        if (e == null) return null;

        AttendanceTypeResponseDTO typeDto = null;
        AttendanceType at = e.getAttendanceType();
        if (at != null) {
            typeDto = new AttendanceTypeResponseDTO(
                    at.getId(),
                    at.getUuid(),
                    at.getTypeName(),
                    at.getShortCode(),
                    at.isPresentMark(),
                    at.isAbsenceMark(),
                    at.isLateMark(),
                    at.getColorCode()
            );
        }

        AbsenceDocumentationSummaryResponseDTO absenceSummary = null;
        AbsenceDocumentation ad = e.getAbsenceDocumentation();
        if (ad != null) {
            absenceSummary = new AbsenceDocumentationSummaryResponseDTO(
                    ad.getId(),
                    ad.getApprovalStatus(),
                    ad.getDocumentationUrl()
            );
        }

        // studentFullName and takenByStaffName are not part of the entity and require external lookup (UIS).
        String studentFullName = null;
        String takenByStaffName = null;

        String studentUuid = null;
        if (e.getStudentId() != null) {
            studentUuid = studentRepository.findById(e.getStudentId())
                    .map(s -> s.getUuid() == null ? null : s.getUuid().toString())
                    .orElse(null);
        }

        String takenByStaffUuid = null;
        if (e.getTakenByStaffId() != null) {
            takenByStaffUuid = staffRepository.findById(e.getTakenByStaffId())
                    .map(s -> s.getUuid() == null ? null : s.getUuid().toString())
                    .orElse(null);
        }

        // IMPORTANT: convert UUID to String to match DTO constructor signature
        String uuidStr = null;
        if (e.getUuid() != null) {
            uuidStr = e.getUuid().toString();
        }

        return new StudentAttendanceResponseDTO(
                e.getId(),                // Long dailyAttendanceId
                uuidStr,                  // String uuid  <- convert UUID -> String
                studentUuid,
                e.getStudentId(),         // Long studentId
                studentFullName,          // String studentFullName
                e.getAttendanceDate(),    // LocalDate attendanceDate
                at == null ? null : at.getShortCode(), // String attendanceTypeShortCode
                takenByStaffUuid,
                e.getTakenByStaffId(),    // Long takenByStaffId
                takenByStaffName,         // String takenByStaffName
                typeDto,                  // AttendanceTypeResponseDTO attendanceType
                e.getNotes(),             // String notes
                absenceSummary,           // AbsenceDocumentationSummaryResponseDTO absenceDocumentation
                e.getCreatedAt(),         // LocalDateTime createdAt
                e.getCreatedBy()          // String createdBy
        );
    }

    private Long resolveStudentId(StudentAttendanceRequestDTO req) {
        if (req.getStudentUuid() != null) {
            return resolveStudentIdFromUuid(req.getStudentUuid());
        }
        if (req.getStudentId() != null) {
            return req.getStudentId();
        }
        throw new AttendanceProcessingException("studentUuid is required (or deprecated studentId during transition)");
    }

    private Long resolveTakenByStaffId(StudentAttendanceRequestDTO req) {
        if (req.getTakenByStaffUuid() != null) {
            return resolveStaffIdFromUuid(req.getTakenByStaffUuid());
        }
        if (req.getTakenByStaffId() != null) {
            return req.getTakenByStaffId();
        }
        throw new AttendanceProcessingException("takenByStaffUuid is required (or deprecated takenByStaffId during transition)");
    }

    private Long resolveStudentIdFromUuid(UUID studentUuid) {
        return studentRepository.findByUuid(studentUuid)
                .map(s -> s.getId())
                .orElseThrow(() -> new AttendanceProcessingException("Student not found for uuid: " + studentUuid));
    }

    private Long resolveStaffIdFromUuid(UUID staffUuid) {
        return staffRepository.findByUuid(staffUuid)
                .map(s -> s.getId())
                .orElseThrow(() -> new AttendanceProcessingException("Staff not found for uuid: " + staffUuid));
    }

    private void validateAttendanceDateWindow(LocalDate attendanceDate) {
        if (attendanceDate == null) {
            throw new AttendanceProcessingException("attendanceDate is required for each attendance record");
        }

        LocalDate today = LocalDate.now();
        if (attendanceDate.isAfter(today)) {
            throw new AttendanceProcessingException("Attendance cannot be marked for a future date");
        }
        if (attendanceDate.isBefore(today.minusDays(7))) {
            throw new AttendanceProcessingException("Attendance cannot be edited or marked for dates older than 7 days");
        }
    }

    private LocalDate parseIsoDate(String isoDate) {
        try {
            return LocalDate.parse(isoDate);
        } catch (DateTimeParseException ex) {
            throw new AttendanceProcessingException("Invalid date format: " + isoDate + ". Expected ISO format yyyy-MM-dd", ex);
        }
    }

    private boolean isNonWorkingStudentDay(LocalDate date) {
        return academicCalendarEventRepository.existsByDateAndDayTypeInAndAppliesToStudentsTrueAndIsActiveTrue(date, NON_WORKING_DAY_TYPES);
    }
}
