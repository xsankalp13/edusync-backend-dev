package com.project.edusync.enrollment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.project.edusync.adm.model.entity.Building;
import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.BuildingRepository;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.common.exception.enrollment.BulkImportException;
import com.project.edusync.common.exception.enrollment.DataParsingException;
import com.project.edusync.common.exception.enrollment.InvalidCsvHeaderException;
import com.project.edusync.common.exception.enrollment.RelatedResourceNotFoundException;
import com.project.edusync.common.exception.enrollment.ResourceDuplicateException;
import com.project.edusync.enrollment.model.dto.BulkImportGuardianInputDTO;
import com.project.edusync.enrollment.model.dto.BulkImportProgressEvent;
import com.project.edusync.enrollment.model.dto.BulkImportReportDTO;
import com.project.edusync.enrollment.model.dto.BulkRoomImportReportDTO;
import com.project.edusync.enrollment.service.BulkImportService;
import com.project.edusync.enrollment.service.SseEmitterRegistry;
import com.project.edusync.enrollment.util.CsvValidationHelper;
import com.project.edusync.enrollment.util.RegisterUserByRole;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.StudentGuardianRelationship;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.enums.*;
import com.project.edusync.uis.repository.GuardianRepository;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentGuardianRelationshipRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service implementation for bulk user import via CSV.
 * This implementation is **resilient** (processing one row at a time) and
 * **optimized** (pre-caches static data like Roles and Sections).
 *
 * It orchestrates the import by:
 * 1. Validating the CSV header structure based on userType.
 * 2. Pre-fetching and caching static data (Roles, Sections) for performance.
 * 3. Looping through the CSV file one row at a time.
 * 4. Calling a separate, transactional method for each row.
 * 5. Wrapping each row's processing in a try-catch block to ensure that
 * one bad row does not stop the entire import.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BulkImportServiceImpl implements BulkImportService {

    // --- Constants ---
    private static final String USER_TYPE_STUDENTS = "students";
    private static final String USER_TYPE_STAFF = "staff";
    private static final String USER_TYPE_STUDENTS_WITH_GUARDIANS = "students-with-guardians";
    private static final String ROLE_STUDENT = "ROLE_STUDENT";
    private static final String ROLE_GUARDIAN = "ROLE_GUARDIAN";

    // --- CSV Header Definitions (NEW) ---
    // (This enforces strict column order for the import)
    private static final List<String> STUDENT_HEADER = Arrays.asList(
            "firstName", "lastName", "middleName", "email", "dateOfBirth",
            "rollNo", "gender", "enrollmentNumber", "enrollmentDate",
            "className", "sectionName"
    );

    // Common staff fields + all *possible* specific fields
    // This provides a single, verifiable header for the "staff.csv"
    private static final List<String> STAFF_HEADER = Arrays.asList(
            // Common Staff (0-10)
            "firstName", "lastName", "middleName", "email", "dateOfBirth",
            "gender", "employeeId", "joiningDate", "jobTitle", "department", "staffType",
            // Teacher (11-15)
            "certifications", "specializations", "yearsOfExperience", "educationLevel", "stateLicenseNumber",
            // Principal (16-17)
            "administrativeCertifications", "schoolLevelManaged",
            // Librarian (18-19)
            "librarySystemPermissions", "mlisDegree",
            // Security (20-21)
            "assignedGate", "shiftTiming"
    );

    private static final List<String> GUARDIAN_HEADER = Arrays.asList(
            "studentEnrollmentNumber", "firstName", "lastName", "middleName", "email", "phoneNumber",
            "relationshipType", "occupation", "employer", "primaryContact", "canPickup", "financialContact", "canViewGrades"
    );

    private static final List<String> ROOM_HEADER = Arrays.asList(
            "name", "roomType", "seatingType", "rowCount", "columnsPerRow", "seatsPerUnit", "floorNumber",
            "building", "hasProjector", "hasAC", "hasWhiteboard", "isAccessible", "otherAmenities"
    );

    private static final Set<String> VALID_ROOM_TYPES = Set.of(
            "CLASSROOM", "LABORATORY", "COMPUTER_LAB", "LIBRARY", "OTHER"
    );

    private static final Set<String> VALID_SEATING_TYPES = Set.of(
            "BENCH", "DESK_CHAIR", "WORKSTATION", "TERMINAL"
    );


    @Value("${edusync.bulk-import.default-password:Welcome@123}")
    private String DEFAULT_PASSWORD;

    // --- Repositories & Services (all final) ---
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final GuardianRepository guardianRepository;
    private final UserProfileRepository userProfileRepository;
    private final StudentGuardianRelationshipRepository studentGuardianRelationshipRepository;
    private final SectionRepository sectionRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final CsvValidationHelper validationHelper;
    private final RegisterUserByRole registerUserByRole;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Emits a progress event to the SSE emitter for the given session.
     * Silently swallows IO errors so that an SSE glitch never aborts the import.
     */
    private void emitEvent(String sessionId, BulkImportProgressEvent event) {
        if (sessionId == null) return;
        SseEmitter emitter = sseEmitterRegistry.get(sessionId);
        if (emitter == null) return;
        try {
            emitter.send(
                SseEmitter.event()
                    .name(event.getEventType())
                    .data(objectMapper.writeValueAsString(event))
            );
        } catch (IOException e) {
            log.warn("Failed to emit SSE event for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Orchestrates the import process.
     * This method is NOT transactional itself.
     */
    @Override
    public BulkImportReportDTO importUsers(MultipartFile file, String userType, String sessionId) throws IOException {

        log.info("Building caches for roles and sections...");
        final Map<String, Role> roleCache = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getName, role -> role));

        final Map<String, Section> sectionCache = sectionRepository.findAllWithClass().stream()
                .collect(Collectors.toMap(
                        s -> s.getAcademicClass().getName() + ":" + s.getSectionName(),
                        s -> s
                ));
        log.info("Caches built with {} roles and {} sections. Starting row processing...", roleCache.size(), sectionCache.size());

        BulkImportReportDTO report = new BulkImportReportDTO();
        report.setStatus("PROCESSING");
        int rowNumber = 1, successCount = 0, failureCount = 0;

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            // --- HEADER VALIDATION ---
            String[] header = csvReader.readNext();
            if (header == null) {
                // USE: InvalidCsvHeaderException (Fatal)
                throw new InvalidCsvHeaderException("File is empty or header is missing.");
            }

            final List<String> expectedHeader;
            if (USER_TYPE_STUDENTS.equalsIgnoreCase(userType)) {
                expectedHeader = STUDENT_HEADER;
            } else if (USER_TYPE_STAFF.equalsIgnoreCase(userType)) {
                expectedHeader = STAFF_HEADER;
            } else {
                throw new BulkImportException("Invalid userType: " + userType, HttpStatus.BAD_REQUEST);
            }

            List<String> actualHeader = Arrays.asList(header);
            if (!actualHeader.equals(expectedHeader)) {
                log.warn("CSV Header Validation FAILED. Expected: {}, Found: {}", expectedHeader, actualHeader);
                // USE: InvalidCsvHeaderException (Fatal)
                throw new InvalidCsvHeaderException(
                        String.format("Invalid CSV header. Expected: %s, Found: %s", expectedHeader, actualHeader)
                );
            }
            log.info("CSV Header validation passed.");
            // --- End Header Validation ---


            String[] row;
            while ((row = csvReader.readNext()) != null) {
                rowNumber++;
                // Extract a human-readable identifier for SSE events (email is col 3)
                String identifier = (row.length > 3 && row[3] != null && !row[3].isBlank())
                        ? row[3] : "row-" + rowNumber;
                try {
                    StudentRowProcessingResult studentResult = null;
                    if (USER_TYPE_STUDENTS.equalsIgnoreCase(userType)) {
                        studentResult = processStudentRow(row, roleCache, sectionCache, Collections.emptyList());
                    } else if (USER_TYPE_STAFF.equalsIgnoreCase(userType)) {
                        routeStaffRowProcessing(row, roleCache);
                    }
                    successCount++;

                    // ── Emit ROW_SUCCESS ──────────────────────────────────────────
                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_SUCCESS")
                            .identifier(identifier)
                            .userType(userType)
                            .studentEnrollmentNumber(studentResult != null ? studentResult.getEnrollmentNumber() : null)
                            .guardiansCreated(studentResult != null ? studentResult.getGuardiansCreatedCount() : 0)
                            .guardiansLinked(studentResult != null ? studentResult.getGuardiansLinkedCount() : 0)
                            .guardianUsernames(studentResult != null ? studentResult.getGuardianUsernames() : Collections.emptyList())
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());

                } catch (Exception e) {
                    failureCount++;
                    String errorMessage = e.getMessage();
                    String error = String.format("Row %d: %s", rowNumber, errorMessage);
                    report.getErrorMessages().add(error);
                    log.warn("Failed to process row {}: {}", rowNumber, errorMessage);

                    // ── Emit ROW_FAILURE ──────────────────────────────────────────
                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_FAILURE")
                            .identifier(identifier)
                            .userType(userType)
                            .errorMessage(errorMessage)
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());
                }
            }
        } catch (CsvValidationException | InvalidCsvHeaderException e) {
            report.setStatus("FAILED");
            report.getErrorMessages().add("Fatal Error: " + e.getMessage());

            // ── Emit JOB_FAILED ───────────────────────────────────────────────────
            emitEvent(sessionId, BulkImportProgressEvent.builder()
                    .eventType("JOB_FAILED")
                    .errorMessage("Fatal Error: " + e.getMessage())
                    .successCount(0)
                    .failureCount(0)
                    .build());
            sseEmitterRegistry.complete(sessionId);
            return report;
        }

        report.setStatus("COMPLETED");
        report.setTotalRows(rowNumber - 1);
        report.setSuccessCount(successCount);
        report.setFailureCount(failureCount);

        // ── Emit JOB_COMPLETE ─────────────────────────────────────────────────────
        emitEvent(sessionId, BulkImportProgressEvent.builder()
                .eventType("JOB_COMPLETE")
                .totalRows(rowNumber - 1)
                .successCount(successCount)
                .failureCount(failureCount)
                .build());
        sseEmitterRegistry.complete(sessionId);
        return report;
    }

    /**
     * Processes and validates a single student row.
     * This method is marked @Transactional.
     */
    @Transactional(rollbackFor = Exception.class)
    public StudentRowProcessingResult processStudentRow(String[] row,
                                                        Map<String, Role> roleCache,
                                                        Map<String, Section> sectionCache,
                                                        List<BulkImportGuardianInputDTO> guardiansForStudent) {
        log.info("[StudentRow] Processing started for candidate enrollmentNumber='{}'", row.length > 7 ? row[7] : "N/A");
        // 1. --- Parse & Validate Data (per students.csv spec) ---
        // This section will now throw DataParsingException if it fails
        String firstName = validationHelper.validateString(row[0], "firstName");
        String lastName = validationHelper.validateString(row[1], "lastName");
        String middleName = row[2];
        String email = validationHelper.validateEmail(row[3]);
        LocalDate dob = validationHelper.parseDate(row[4], "dateOfBirth");
        Integer rollNo = validationHelper.parseInt(row[5], "rollNo");
        Gender gender = validationHelper.parseEnum(Gender.class, row[6], "gender");
        String enrollmentNumber = validationHelper.validateString(row[7], "enrollmentNumber");
        LocalDate enrollmentDate = validationHelper.parseDate(row[8], "enrollmentDate");
        String className = validationHelper.validateString(row[9], "className");
        String sectionName = validationHelper.validateString(row[10], "sectionName");

        // 2. --- Validate Business Logic & Foreign Keys ---
        if (userRepository.existsByUsername(enrollmentNumber)) {
            // USE: ResourceDuplicateException
            throw new ResourceDuplicateException("User with username '" + enrollmentNumber + "' already exists.");
        }
        if (userRepository.existsByEmail(email)) {
            // USE: ResourceDuplicateException
            throw new ResourceDuplicateException("User with email '" + email + "' already exists.");
        }
        if (studentRepository.existsByEnrollmentNumber(enrollmentNumber)) {
            // USE: ResourceDuplicateException
            throw new ResourceDuplicateException("Student with enrollment number '" + enrollmentNumber + "' already exists.");
        }

        Section section = sectionCache.get(className + ":" + sectionName);
        if (section == null) {
            // USE: RelatedResourceNotFoundException
            throw new RelatedResourceNotFoundException("Section not found for class '" + className + "' and section '" + sectionName + "'.");
        }

        Role studentRole = roleCache.get(ROLE_STUDENT);
        if (studentRole == null) {
            // USE: RelatedResourceNotFoundException
            throw new RelatedResourceNotFoundException("CRITICAL: " + ROLE_STUDENT + " not found in database.");
        }

        log.info("[StudentRow] Validation passed for enrollmentNumber='{}'; creating student user/profile/entity", enrollmentNumber);

        // 3. --- Delegate creation to the helper ---
        Student student = registerUserByRole.RegisterStudent(
                email, enrollmentNumber, DEFAULT_PASSWORD, studentRole,
                firstName, lastName, middleName, dob, gender,
                enrollmentDate, section, rollNo
        );

        StudentRowProcessingResult rowResult = new StudentRowProcessingResult(enrollmentNumber);
        if (guardiansForStudent == null || guardiansForStudent.isEmpty()) {
            log.info("[StudentRow] No guardians provided for enrollmentNumber='{}'; row completed", enrollmentNumber);
            return rowResult;
        }

        log.info("[StudentRow] Found {} guardian row(s) for enrollmentNumber='{}'", guardiansForStudent.size(), enrollmentNumber);

        for (BulkImportGuardianInputDTO guardianInput : guardiansForStudent) {
            log.info("[StudentRow] Resolving guardian for student='{}' with guardianEmail='{}' guardianPhone='{}'",
                    enrollmentNumber, guardianInput.getEmail(), guardianInput.getPhoneNumber());
            GuardianResolutionResult resolved = resolveOrCreateGuardian(guardianInput, roleCache);
            upsertStudentGuardianRelationship(student, resolved.getGuardian(), guardianInput);

            if (resolved.isCreated()) {
                rowResult.incrementGuardiansCreated();
            }
            rowResult.incrementGuardiansLinked();
            rowResult.addGuardianUsername(resolved.getGuardianUsername());
            log.info("[StudentRow] Guardian linked for student='{}', guardianUsername='{}', createdNow='{}'",
                    enrollmentNumber, resolved.getGuardianUsername(), resolved.isCreated());
        }
        rowResult.sortGuardianUsernames();
        log.info("[StudentRow] Completed enrollmentNumber='{}'; guardiansCreated={}, guardiansLinked={}",
                enrollmentNumber, rowResult.getGuardiansCreatedCount(), rowResult.getGuardiansLinkedCount());
        return rowResult;
    }

    @Override
    public BulkImportReportDTO importStudentsWithGuardians(MultipartFile studentsFile,
                                                           MultipartFile guardiansFile,
                                                           String sessionId) throws IOException {
        log.info("[StudentsWithGuardians] Import started. studentsFile='{}', guardiansFile='{}', sessionId='{}'",
                studentsFile.getOriginalFilename(), guardiansFile.getOriginalFilename(), sessionId);
        log.info("[StudentsWithGuardians] Building caches for roles and sections...");
        final Map<String, Role> roleCache = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getName, role -> role));
        final Map<String, Section> sectionCache = sectionRepository.findAllWithClass().stream()
                .collect(Collectors.toMap(
                        s -> s.getAcademicClass().getName() + ":" + s.getSectionName(),
                        s -> s
                ));
        log.info("[StudentsWithGuardians] Cache build complete: roles={}, sections={}", roleCache.size(), sectionCache.size());

        Map<String, List<BulkImportGuardianInputDTO>> guardiansByEnrollment = parseGuardiansFile(guardiansFile);
        log.info("[StudentsWithGuardians] Guardians parsed successfully. Distinct student references={}", guardiansByEnrollment.size());
        Set<String> matchedEnrollmentNumbers = new HashSet<>();

        BulkImportReportDTO report = new BulkImportReportDTO();
        report.setStatus("PROCESSING");
        int rowNumber = 1;
        int successCount = 0;
        int failureCount = 0;

        try (Reader reader = new InputStreamReader(studentsFile.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            if (header == null) {
                throw new InvalidCsvHeaderException("students.csv is empty or header is missing.");
            }

            List<String> actualHeader = Arrays.asList(header);
            if (!actualHeader.equals(STUDENT_HEADER)) {
                throw new InvalidCsvHeaderException(
                        String.format("Invalid students.csv header. Expected: %s, Found: %s", STUDENT_HEADER, actualHeader)
                );
            }
            log.info("[StudentsWithGuardians] students.csv header validation passed.");

            String[] row;
            while ((row = csvReader.readNext()) != null) {
                rowNumber++;
                String identifier = (row.length > 7 && row[7] != null && !row[7].isBlank()) ? row[7] : "row-" + rowNumber;
                try {
                    String enrollmentNumber = row.length > 7 ? row[7].trim() : "";
                    List<BulkImportGuardianInputDTO> guardians = guardiansByEnrollment.getOrDefault(
                            enrollmentNumber,
                            Collections.emptyList()
                    );
                    log.info("[StudentsWithGuardians] Processing row={} enrollmentNumber='{}' guardiansAttached={}",
                            rowNumber - 1, enrollmentNumber, guardians.size());

                    StudentRowProcessingResult result = processStudentRow(row, roleCache, sectionCache, guardians);
                    matchedEnrollmentNumbers.add(enrollmentNumber);
                    successCount++;

                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_SUCCESS")
                            .identifier(identifier)
                            .userType(USER_TYPE_STUDENTS_WITH_GUARDIANS)
                            .studentEnrollmentNumber(result.getEnrollmentNumber())
                            .guardiansCreated(result.getGuardiansCreatedCount())
                            .guardiansLinked(result.getGuardiansLinkedCount())
                            .guardianUsernames(result.getGuardianUsernames())
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());
                    log.info("[StudentsWithGuardians] Row={} succeeded for enrollmentNumber='{}' (successCount={}, failureCount={})",
                            rowNumber - 1, enrollmentNumber, successCount, failureCount);
                } catch (Exception e) {
                    failureCount++;
                    String errorMessage = e.getMessage();
                    report.getErrorMessages().add(String.format("Row %d: %s", rowNumber, errorMessage));
                    log.warn("[StudentsWithGuardians] Row={} failed. Reason: {}", rowNumber - 1, errorMessage);

                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_FAILURE")
                            .identifier(identifier)
                            .userType(USER_TYPE_STUDENTS_WITH_GUARDIANS)
                            .errorMessage(errorMessage)
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());
                }
            }

            Set<String> unmatchedEnrollmentNumbers = new HashSet<>(guardiansByEnrollment.keySet());
            unmatchedEnrollmentNumbers.removeAll(matchedEnrollmentNumbers);

            for (String unmatched : unmatchedEnrollmentNumbers) {
                rowNumber++;
                String identifier = unmatched;
                log.info("[StudentsWithGuardians] Processing unmatched guardian references for student='{}'", unmatched);

                Optional<Student> existingStudentOpt = studentRepository.findByEnrollmentNumber(unmatched);
                if (existingStudentOpt.isPresent()) {
                    Student existingStudent = existingStudentOpt.get();
                    List<BulkImportGuardianInputDTO> guardians = guardiansByEnrollment.get(unmatched);
                    try {
                        StudentRowProcessingResult result = processGuardiansForExistingStudent(existingStudent, guardians, roleCache);
                        successCount++;
                        emitEvent(sessionId, BulkImportProgressEvent.builder()
                                .rowNumber(rowNumber - 1)
                                .eventType("ROW_SUCCESS")
                                .identifier(identifier)
                                .userType(USER_TYPE_STUDENTS_WITH_GUARDIANS)
                                .studentEnrollmentNumber(result.getEnrollmentNumber())
                                .guardiansCreated(result.getGuardiansCreatedCount())
                                .guardiansLinked(result.getGuardiansLinkedCount())
                                .guardianUsernames(result.getGuardianUsernames())
                                .successCount(successCount)
                                .failureCount(failureCount)
                                .build());
                        log.info("[StudentsWithGuardians] Row={} succeeded for existing student='{}'", rowNumber - 1, identifier);
                    } catch (Exception e) {
                        failureCount++;
                        String errorMessage = e.getMessage();
                        report.getErrorMessages().add(String.format("Student '%s': %s", identifier, errorMessage));
                        log.warn("[StudentsWithGuardians] Failed to link guardians for existing student='{}'. Reason: {}", identifier, errorMessage);
                        emitEvent(sessionId, BulkImportProgressEvent.builder()
                                .rowNumber(rowNumber - 1)
                                .eventType("ROW_FAILURE")
                                .identifier(identifier)
                                .userType(USER_TYPE_STUDENTS_WITH_GUARDIANS)
                                .errorMessage(errorMessage)
                                .successCount(successCount)
                                .failureCount(failureCount)
                                .build());
                    }
                } else {
                    failureCount++;
                    String errorMessage = "Guardians file references unknown studentEnrollmentNumber '" + unmatched + "'.";
                    report.getErrorMessages().add(errorMessage);
                    log.warn("[StudentsWithGuardians] {}", errorMessage);
                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_FAILURE")
                            .identifier(identifier)
                            .userType(USER_TYPE_STUDENTS_WITH_GUARDIANS)
                            .errorMessage(errorMessage)
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());
                }
            }
        } catch (CsvValidationException | InvalidCsvHeaderException e) {
            report.setStatus("FAILED");
            report.getErrorMessages().add("Fatal Error: " + e.getMessage());
            log.error("[StudentsWithGuardians] Fatal import failure: {}", e.getMessage());
            emitEvent(sessionId, BulkImportProgressEvent.builder()
                    .eventType("JOB_FAILED")
                    .userType(USER_TYPE_STUDENTS_WITH_GUARDIANS)
                    .errorMessage("Fatal Error: " + e.getMessage())
                    .successCount(0)
                    .failureCount(0)
                    .build());
            sseEmitterRegistry.complete(sessionId);
            return report;
        }

        report.setStatus("COMPLETED");
        report.setTotalRows(rowNumber - 1);
        report.setSuccessCount(successCount);
        report.setFailureCount(failureCount);

        emitEvent(sessionId, BulkImportProgressEvent.builder()
                .eventType("JOB_COMPLETE")
                .userType(USER_TYPE_STUDENTS_WITH_GUARDIANS)
                .totalRows(rowNumber - 1)
                .successCount(successCount)
                .failureCount(failureCount)
                .build());
        sseEmitterRegistry.complete(sessionId);
        log.info("[StudentsWithGuardians] Import completed. totalRows={}, successCount={}, failureCount={}",
                rowNumber - 1, successCount, failureCount);
        return report;
    }

    @Override
    public BulkRoomImportReportDTO importRooms(MultipartFile file, String sessionId) throws IOException {
        int rowNumber = 1;
        int successCount = 0;
        int failureCount = 0;
        Set<String> errorMessages = new LinkedHashSet<>();

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            if (header == null) {
                throw new InvalidCsvHeaderException("File is empty or header is missing.");
            }
            List<String> actualHeader = Arrays.asList(header);
            if (!actualHeader.equals(ROOM_HEADER)) {
                throw new InvalidCsvHeaderException(
                        String.format("Invalid CSV header. Expected: %s, Found: %s", ROOM_HEADER, actualHeader)
                );
            }

            String[] row;
            while ((row = csvReader.readNext()) != null) {
                rowNumber++;
                String identifier = row.length > 0 && row[0] != null && !row[0].isBlank()
                        ? row[0].trim()
                        : "row-" + rowNumber;

                try {
                    Integer capacity = processRoomRow(row);
                    successCount++;

                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_SUCCESS")
                            .identifier(identifier)
                            .userType("rooms")
                            .totalCapacity(capacity)
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());
                } catch (BulkImportException e) {
                    failureCount++;
                    String rowError = String.format("Row %d: %s", rowNumber, e.getMessage());
                    errorMessages.add(rowError);

                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_FAILURE")
                            .identifier(identifier)
                            .userType("rooms")
                            .errorMessage(e.getMessage())
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());
                } catch (DataIntegrityViolationException e) {
                    failureCount++;
                    String message = "Database constraint violation while saving room.";
                    String rowError = String.format("Row %d: %s", rowNumber, message);
                    errorMessages.add(rowError);

                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_FAILURE")
                            .identifier(identifier)
                            .userType("rooms")
                            .errorMessage(message)
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());
                } catch (Exception e) {
                    failureCount++;
                    String message = "Unexpected error while processing room row.";
                    String rowError = String.format("Row %d: %s", rowNumber, message);
                    errorMessages.add(rowError);
                    log.error("Unexpected room import error at row {}", rowNumber, e);

                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_FAILURE")
                            .identifier(identifier)
                            .userType("rooms")
                            .errorMessage(message)
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());
                }
            }
        } catch (CsvValidationException | BulkImportException e) {
            String fatal = "Fatal Error: " + e.getMessage();
            errorMessages.add(fatal);

            emitEvent(sessionId, BulkImportProgressEvent.builder()
                    .eventType("JOB_FAILED")
                    .userType("rooms")
                    .errorMessage(fatal)
                    .successCount(0)
                    .failureCount(0)
                    .build());
            sseEmitterRegistry.complete(sessionId);
            return new BulkRoomImportReportDTO("FAILED", 0, 0, 0, errorMessages.stream().toList());
        }

        int totalRows = rowNumber - 1;
        String status = successCount == 0
                ? "FAILED"
                : (failureCount == 0 ? "SUCCESS" : "PARTIAL");

        emitEvent(sessionId, BulkImportProgressEvent.builder()
                .eventType("JOB_COMPLETE")
                .userType("rooms")
                .totalRows(totalRows)
                .successCount(successCount)
                .failureCount(failureCount)
                .errorMessages(errorMessages.stream().toList())
                .build());
        sseEmitterRegistry.complete(sessionId);

        return new BulkRoomImportReportDTO(
                status,
                totalRows,
                successCount,
                failureCount,
                errorMessages.stream().toList()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer processRoomRow(String[] row) {
        String name = validationHelper.validateString(getRequiredColumn(row, 0, "name"), "name");
        String roomType = validationHelper.validateString(getRequiredColumn(row, 1, "roomType"), "roomType").toUpperCase(Locale.ROOT);
        String seatingType = validationHelper.validateString(getRequiredColumn(row, 2, "seatingType"), "seatingType").toUpperCase(Locale.ROOT);

        if (!VALID_ROOM_TYPES.contains(roomType)) {
            throw new BulkImportException("Invalid roomType '" + roomType + "'.", HttpStatus.BAD_REQUEST);
        }
        if (!VALID_SEATING_TYPES.contains(seatingType)) {
            throw new BulkImportException("Invalid seatingType '" + seatingType + "'.", HttpStatus.BAD_REQUEST);
        }

        Integer rowCount = parseMinInteger(row, 3, "rowCount", 1);
        Integer columnsPerRow = parseMinInteger(row, 4, "columnsPerRow", 1);
        Integer seatsPerUnit = parseMinInteger(row, 5, "seatsPerUnit", 1);
        Integer floorNumber = parseMinInteger(row, 6, "floorNumber", 0);
        String buildingName = validationHelper.validateString(getRequiredColumn(row, 7, "building"), "building");

        Building building = buildingRepository.findByNameIgnoreCase(buildingName)
                .orElseThrow(() -> new RelatedResourceNotFoundException("Building '" + buildingName + "' not found"));

        if (roomRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceDuplicateException("Duplicate room name '" + name + "'");
        }

        Room room = new Room();
        room.setName(name);
        room.setRoomType(roomType);
        room.setSeatingType(seatingType);
        room.setRowCount(rowCount);
        room.setColumnsPerRow(columnsPerRow);
        room.setSeatsPerUnit(seatsPerUnit);
        room.setFloorNumber(floorNumber);
        room.setBuilding(building);
        room.setHasProjector(parseOptionalBoolean(row, 8, false, "hasProjector"));
        room.setHasAC(parseOptionalBoolean(row, 9, false, "hasAC"));
        room.setHasWhiteboard(parseOptionalBoolean(row, 10, true, "hasWhiteboard"));
        room.setIsAccessible(parseOptionalBoolean(row, 11, false, "isAccessible"));
        room.setOtherAmenities(parseOptionalString(row, 12, "otherAmenities"));
        room.setIsActive(true);

        Room saved = roomRepository.save(room);
        return saved.getTotalCapacity();
    }

    private String getRequiredColumn(String[] row, int index, String fieldName) {
        if (index >= row.length) {
            throw new BulkImportException("Missing required column '" + fieldName + "'.", HttpStatus.BAD_REQUEST);
        }
        return row[index];
    }

    private Integer parseMinInteger(String[] row, int index, String fieldName, int minValue) {
        Integer value = validationHelper.parseInt(getRequiredColumn(row, index, fieldName), fieldName);
        if (value < minValue) {
            throw new BulkImportException(fieldName + " must be >= " + minValue + ".", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private Boolean parseOptionalBoolean(String[] row, int index, boolean defaultValue, String fieldName) {
        if (index >= row.length || row[index] == null || row[index].isBlank()) {
            return defaultValue;
        }
        return validationHelper.parseBoolean(row[index], fieldName);
    }

    private String parseOptionalString(String[] row, int index, String fieldName) {
        if (index >= row.length || row[index] == null || row[index].isBlank()) {
            return null;
        }
        String value = row[index].trim();
        if ("otherAmenities".equals(fieldName) && value.length() > 500) {
            throw new DataParsingException("otherAmenities must be at most 500 characters.");
        }
        return value;
    }

    private Map<String, List<BulkImportGuardianInputDTO>> parseGuardiansFile(MultipartFile guardiansFile) throws IOException {
        log.info("[GuardiansCsv] Parsing started for file='{}'", guardiansFile.getOriginalFilename());
        Map<String, List<BulkImportGuardianInputDTO>> guardiansByEnrollment = new HashMap<>();

        try (Reader reader = new InputStreamReader(guardiansFile.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            if (header == null) {
                log.info("[GuardiansCsv] File has no rows beyond header; continuing with zero guardians.");
                return guardiansByEnrollment;
            }
            List<String> actualHeader = Arrays.asList(header);
            if (!actualHeader.equals(GUARDIAN_HEADER)) {
                throw new InvalidCsvHeaderException(
                        String.format("Invalid guardians.csv header. Expected: %s, Found: %s", GUARDIAN_HEADER, actualHeader)
                );
            }
            log.info("[GuardiansCsv] Header validation passed.");

            String[] row;
            int rowNumber = 1;
            while ((row = csvReader.readNext()) != null) {
                rowNumber++;
                try {
                    BulkImportGuardianInputDTO dto = new BulkImportGuardianInputDTO();
                    dto.setStudentEnrollmentNumber(validationHelper.validateString(row[0], "studentEnrollmentNumber"));
                    dto.setFirstName(validationHelper.validateString(row[1], "guardian.firstName"));
                    dto.setLastName(validationHelper.validateString(row[2], "guardian.lastName"));
                    dto.setMiddleName(row[3]);
                    dto.setEmail(validationHelper.validateEmail(row[4]));
                    dto.setPhoneNumber(normalizePhoneToUsername(row[5]));
                    dto.setRelationshipType(validationHelper.validateString(row[6], "guardian.relationshipType"));
                    dto.setOccupation(trimOptional(row[7]));
                    dto.setEmployer(trimOptional(row[8]));
                    dto.setPrimaryContact(parseBooleanOrDefault(row, 9, "guardian.primaryContact"));
                    dto.setCanPickup(parseBooleanOrDefault(row, 10, "guardian.canPickup"));
                    dto.setFinancialContact(parseBooleanOrDefault(row, 11, "guardian.financialContact"));
                    dto.setCanViewGrades(parseBooleanOrDefault(row, 12, "guardian.canViewGrades"));

                    guardiansByEnrollment
                            .computeIfAbsent(dto.getStudentEnrollmentNumber(), key -> new java.util.ArrayList<>())
                            .add(dto);
                    log.debug("[GuardiansCsv] Parsed row={} for studentEnrollmentNumber='{}' guardianEmail='{}' guardianPhone='{}'",
                            rowNumber - 1, dto.getStudentEnrollmentNumber(), dto.getEmail(), dto.getPhoneNumber());
                } catch (Exception e) {
                    log.warn("[GuardiansCsv] Invalid row={} reason='{}'", rowNumber - 1, e.getMessage());
                    throw new BulkImportException("guardians.csv row " + rowNumber + " invalid: " + e.getMessage(), HttpStatus.BAD_REQUEST);
                }
            }
        } catch (CsvValidationException e) {
            log.error("[GuardiansCsv] CSV parsing error: {}", e.getMessage());
            throw new BulkImportException("Error reading guardians.csv: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        log.info("[GuardiansCsv] Parsing completed. Distinct student references={}, total guardian rows={}",
                guardiansByEnrollment.size(), guardiansByEnrollment.values().stream().mapToInt(List::size).sum());
        return guardiansByEnrollment;
    }

    private boolean parseBooleanOrDefault(String[] row, int index, String fieldName) {
        if (index >= row.length || row[index] == null || row[index].isBlank()) {
            return false;
        }
        return validationHelper.parseBoolean(row[index], fieldName);
    }

    private GuardianResolutionResult resolveOrCreateGuardian(BulkImportGuardianInputDTO guardianInput,
                                                             Map<String, Role> roleCache) {
        String email = validationHelper.validateEmail(guardianInput.getEmail());
        String phoneUsername = normalizePhoneToUsername(guardianInput.getPhoneNumber());
        log.info("[GuardianResolve] Resolving guardian by phone='{}' or email='{}'", phoneUsername, email);

        Optional<User> byPhone = userRepository.findByUsername(phoneUsername);
        Optional<User> byEmail = userRepository.findByEmail(email);

        if (byPhone.isPresent() && byEmail.isPresent() && !byPhone.get().getId().equals(byEmail.get().getId())) {
            log.warn("[GuardianResolve] Conflict: phone/email resolve to different users. phone='{}', email='{}'", phoneUsername, email);
            throw new ResourceDuplicateException(
                    "Guardian phone and email are mapped to different users. phone='" + phoneUsername + "', email='" + email + "'."
            );
        }

        User existingUser = byPhone.orElseGet(() -> byEmail.orElse(null));
        if (existingUser != null) {
            log.info("[GuardianResolve] Existing guardian user found (userId={}); reusing", existingUser.getId());
            UserProfile userProfile = userProfileRepository.findByUser(existingUser)
                    .orElseThrow(() -> new RelatedResourceNotFoundException(
                            "Guardian profile missing for existing user '" + existingUser.getUsername() + "'."
                    ));
            Guardian guardian = guardianRepository.findByUserProfile(userProfile)
                    .orElseGet(() -> {
                        Guardian created = new Guardian();
                        created.setUserProfile(userProfile);
                        created.setActive(true);
                        return created;
                    });

            guardian.setPhoneNumber(phoneUsername);
            guardian.setOccupation(trimOptional(guardianInput.getOccupation()));
            guardian.setEmployer(trimOptional(guardianInput.getEmployer()));

            Guardian saved = guardianRepository.save(guardian);
            log.info("[GuardianResolve] Existing guardian profile saved/reused with uuid='{}'", saved.getUuid());
            return new GuardianResolutionResult(saved, false, phoneUsername);
        }

        Role guardianRole = roleCache.get(ROLE_GUARDIAN);
        if (guardianRole == null) {
            throw new RelatedResourceNotFoundException("CRITICAL: " + ROLE_GUARDIAN + " not found in database.");
        }

        Guardian createdGuardian = registerUserByRole.RegisterGuardian(
                email,
                phoneUsername,
                DEFAULT_PASSWORD,
                guardianRole,
                validationHelper.validateString(guardianInput.getFirstName(), "guardian.firstName"),
                validationHelper.validateString(guardianInput.getLastName(), "guardian.lastName"),
                trimOptional(guardianInput.getMiddleName()),
                trimOptional(guardianInput.getOccupation()),
                trimOptional(guardianInput.getEmployer())
        );
        log.info("[GuardianResolve] New guardian created with uuid='{}' and username='{}'", createdGuardian.getUuid(), phoneUsername);
        return new GuardianResolutionResult(createdGuardian, true, phoneUsername);
    }

    private void upsertStudentGuardianRelationship(Student student,
                                                   Guardian guardian,
                                                   BulkImportGuardianInputDTO input) {
        log.info("[GuardianLink] Upserting link studentId='{}' guardianId='{}' relationship='{}'",
                student.getId(), guardian.getId(), input.getRelationshipType());
        StudentGuardianRelationship relation = studentGuardianRelationshipRepository
                .findByStudentAndGuardian(student, guardian)
                .orElseGet(() -> {
                    StudentGuardianRelationship newRelation = new StudentGuardianRelationship();
                    newRelation.setStudent(student);
                    newRelation.setGuardian(guardian);
                    return newRelation;
                });

        relation.setRelationshipType(validationHelper.validateString(input.getRelationshipType(), "guardian.relationshipType"));
        relation.setPrimaryContact(input.isPrimaryContact());
        relation.setCanPickup(input.isCanPickup());
        relation.setFinancialContact(input.isFinancialContact());
        relation.setCanViewGrades(input.isCanViewGrades());
        studentGuardianRelationshipRepository.save(relation);
        log.info("[GuardianLink] Link upsert completed studentId='{}' guardianId='{}'", student.getId(), guardian.getId());
    }

    /**
     * Processes guardian logic for an *existing* student (used when guardians-only import happens).
     * Marked Transactional to ensure that if anything fails during the link, it rolls back.
     */
    @Transactional(rollbackFor = Exception.class)
    public StudentRowProcessingResult processGuardiansForExistingStudent(Student student,
                                                                         List<BulkImportGuardianInputDTO> guardiansForStudent,
                                                                         Map<String, Role> roleCache) {
        String enrollmentNumber = student.getEnrollmentNumber();
        log.info("[GuardianOnly] Processing started for existing student enrollmentNumber='{}'", enrollmentNumber);

        StudentRowProcessingResult rowResult = new StudentRowProcessingResult(enrollmentNumber);
        if (guardiansForStudent == null || guardiansForStudent.isEmpty()) {
            return rowResult; // No-op, but successful
        }

        for (BulkImportGuardianInputDTO guardianInput : guardiansForStudent) {
            log.info("[GuardianOnly] Resolving guardian for student='{}' with email='{}' phone='{}'",
                    enrollmentNumber, guardianInput.getEmail(), guardianInput.getPhoneNumber());
            GuardianResolutionResult resolved = resolveOrCreateGuardian(guardianInput, roleCache);
            upsertStudentGuardianRelationship(student, resolved.getGuardian(), guardianInput);

            if (resolved.isCreated()) {
                rowResult.incrementGuardiansCreated();
            }
            rowResult.incrementGuardiansLinked();
            rowResult.addGuardianUsername(resolved.getGuardianUsername());
            log.info("[GuardianOnly] Guardian linked for student='{}', guardianUsername='{}', createdNow='{}'",
                    enrollmentNumber, resolved.getGuardianUsername(), resolved.isCreated());
        }
        rowResult.sortGuardianUsernames();
        log.info("[GuardianOnly] Completed enrollmentNumber='{}'; guardiansCreated={}, guardiansLinked={}",
                enrollmentNumber, rowResult.getGuardiansCreatedCount(), rowResult.getGuardiansLinkedCount());
        return rowResult;
    }

    private String normalizePhoneToUsername(String rawPhone) {
        String validated = validationHelper.validatePhoneNumber(rawPhone, "guardian.phoneNumber");
        return validated.replaceAll("[\\s\\-()]+", "");
    }

    private String trimOptional(String value) {
        return value == null ? null : value.trim();
    }

    public static class StudentRowProcessingResult {
        private final String enrollmentNumber;
        private int guardiansCreatedCount;
        private int guardiansLinkedCount;
        private final List<String> guardianUsernames = new java.util.ArrayList<>();

        public StudentRowProcessingResult(String enrollmentNumber) {
            this.enrollmentNumber = enrollmentNumber;
        }

        public String getEnrollmentNumber() {
            return enrollmentNumber;
        }

        public int getGuardiansCreatedCount() {
            return guardiansCreatedCount;
        }

        public int getGuardiansLinkedCount() {
            return guardiansLinkedCount;
        }

        public List<String> getGuardianUsernames() {
            return guardianUsernames;
        }

        public void incrementGuardiansCreated() {
            guardiansCreatedCount++;
        }

        public void incrementGuardiansLinked() {
            guardiansLinkedCount++;
        }

        public void addGuardianUsername(String username) {
            guardianUsernames.add(username);
        }

        public void sortGuardianUsernames() {
            guardianUsernames.sort(String::compareTo);
        }
    }

    private static class GuardianResolutionResult {
        private final Guardian guardian;
        private final boolean created;
        private final String guardianUsername;

        private GuardianResolutionResult(Guardian guardian, boolean created, String guardianUsername) {
            this.guardian = guardian;
            this.created = created;
            this.guardianUsername = guardianUsername;
        }

        public Guardian getGuardian() {
            return guardian;
        }

        public boolean isCreated() {
            return created;
        }

        public String getGuardianUsername() {
            return guardianUsername;
        }
    }


    /**
     * (NEW) Transactional router for staff processing.
     * This method is the single transactional entry point for a staff row.
     * It parses *only* the staffType to determine which specific
     * processing method to call.
     *
     * @param row The raw String[] from the CSV.
     * @param roleCache The pre-fetched Role map.
     * @throws Exception if any validation or database constraint fails.
     */
    @Transactional(rollbackFor = Exception.class)
    public void routeStaffRowProcessing(String[] row, Map<String, Role> roleCache) throws Exception {
        // This will throw DataParsingException if row[10] is invalid
        StaffType staffType = validationHelper.parseEnum(StaffType.class, row[10], "staffType");

        switch (staffType) {
            case TEACHER:
                processTeacherRow(row, roleCache);
                break;
            case PRINCIPAL:
                processPrincipalRow(row, roleCache);
                break;
            case LIBRARIAN:
                processLibrarianRow(row, roleCache);
                break;
            default:
                throw new BulkImportException("Unsupported staff type '" + staffType + "' for bulk import.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * (NEW) Private helper to process and save a single Teacher row.
     * This method is NOT transactional; it runs inside the transaction
     * of `routeStaffRowProcessing`.
     */
    private void processTeacherRow(String[] row, Map<String, Role> roleCache) throws Exception {
        // 1. --- Parse Common Staff Fields (Indices 0-9) ---
        String firstName = validationHelper.validateString(row[0], "firstName");
        String lastName = validationHelper.validateString(row[1], "lastName");
        String middleName = row[2]; // Optional
        String email = validationHelper.validateEmail(row[3]);
        LocalDate dob = validationHelper.parseDate(row[4], "dateOfBirth");
        Gender gender = validationHelper.parseEnum(Gender.class, row[5], "gender");
        String employeeId = validationHelper.validateString(row[6], "employeeId");
        LocalDate joiningDate = validationHelper.parseDate(row[7], "joiningDate");
        String jobTitle = validationHelper.validateString(row[8], "jobTitle");
        Department department = validationHelper.parseEnum(Department.class, row[9], "department");

        // 2. --- Parse Teacher-Specific Fields (Indices 11-15) ---
        String certifications = row[11]; // Assuming JSON string or CSV
        String specializations = row[12]; // Assuming JSON string or CSV
        Integer yearsOfExperience = validationHelper.parseInt(row[13], "yearsOfExperience");
        EducationLevel educationLevel = validationHelper.parseEnum(
                EducationLevel.class, row[14], "educationLevel"
        );
        String stateLicenseNumber = row[15];

        // 3. --- Validate Business Logic ---
        if (userRepository.existsByEmail(email)) {
            throw new ResourceDuplicateException("User with email '" + email + "' already exists.");
        }
        if (staffRepository.existsByEmployeeId(employeeId)) {
            throw new ResourceDuplicateException("Staff with employee ID '" + employeeId + "' already exists.");
        }

        Role staffRole = roleCache.get("ROLE_TEACHER");
        if (staffRole == null) {
            throw new RelatedResourceNotFoundException("CRITICAL: Role 'ROLE_TEACHER' not found in database.");
        }

        // 4. --- Delegate creation to the (refactored) helper ---
        registerUserByRole.RegisterStaff(email, employeeId, DEFAULT_PASSWORD, staffRole,
                firstName, lastName, middleName, dob, gender, joiningDate, jobTitle,
                department, StaffType.TEACHER, row);
    }

    /**
     * (NEW) Private helper to process and save a single Principal row.
     */
    private void processPrincipalRow(String[] row, Map<String, Role> roleCache) throws Exception {
        // 1. --- Parse Common Staff Fields (Indices 0-9) ---
        String firstName = validationHelper.validateString(row[0], "firstName");
        String lastName = validationHelper.validateString(row[1], "lastName");
        String middleName = row[2]; // Optional
        String email = validationHelper.validateEmail(row[3]);
        LocalDate dob = validationHelper.parseDate(row[4], "dateOfBirth");
        Gender gender = validationHelper.parseEnum(Gender.class, row[5], "gender");
        String employeeId = validationHelper.validateString(row[6], "employeeId");
        LocalDate joiningDate = validationHelper.parseDate(row[7], "joiningDate");
        String jobTitle = validationHelper.validateString(row[8], "jobTitle");
        Department department = validationHelper.parseEnum(Department.class, row[9], "department");

        // 2. --- Parse Principal-Specific Fields (Indices 16-17) ---
        String adminCertifications = row[16]; // Assuming JSON string or CSV
        SchoolLevel schoolLevel = validationHelper.parseEnum(
                SchoolLevel.class, row[17], "schoolLevelManaged"
        );

        // 3. --- Validate Business Logic ---
        if (userRepository.existsByEmail(email)) {
            throw new ResourceDuplicateException("User with email '" + email + "' already exists.");
        }
        if (staffRepository.existsByEmployeeId(employeeId)) {
            throw new ResourceDuplicateException("Staff with employee ID '" + employeeId + "' already exists.");
        }

        Role staffRole = roleCache.get("ROLE_PRINCIPAL");
        if (staffRole == null) {
            throw new RelatedResourceNotFoundException("CRITICAL: Role 'ROLE_PRINCIPAL' not found in database.");
        }

        // 4. --- Delegate creation to the (refactored) helper ---
        registerUserByRole.RegisterStaff(email, employeeId, DEFAULT_PASSWORD, staffRole,
                firstName, lastName, middleName, dob, gender, joiningDate, jobTitle,
                department, StaffType.PRINCIPAL, row);
    }

    /**
     * (NEW) Private helper to process and save a single Librarian row.
     */
    private void processLibrarianRow(String[] row, Map<String, Role> roleCache) throws Exception {
        // 1. --- Parse Common Staff Fields (Indices 0-9) ---
        String firstName = validationHelper.validateString(row[0], "firstName");
        String lastName = validationHelper.validateString(row[1], "lastName");
        String middleName = row[2]; // Optional
        String email = validationHelper.validateEmail(row[3]);
        LocalDate dob = validationHelper.parseDate(row[4], "dateOfBirth");
        Gender gender = validationHelper.parseEnum(Gender.class, row[5], "gender");
        String employeeId = validationHelper.validateString(row[6], "employeeId");
        LocalDate joiningDate = validationHelper.parseDate(row[7], "joiningDate");
        String jobTitle = validationHelper.validateString(row[8], "jobTitle");
        Department department = validationHelper.parseEnum(Department.class, row[9], "department");

        // 3. --- Validate Business Logic ---
        if (userRepository.existsByEmail(email)) {
            throw new ResourceDuplicateException("User with email '" + email + "' already exists.");
        }
        if (staffRepository.existsByEmployeeId(employeeId)) {
            throw new ResourceDuplicateException("Staff with employee ID '" + employeeId + "' already exists.");
        }

        Role staffRole = roleCache.get("ROLE_LIBRARIAN");
        if (staffRole == null) {
            throw new RelatedResourceNotFoundException("CRITICAL: Role 'ROLE_LIBRARIAN' not found in database.");
        }

        // 4. --- Delegate creation to the (refactored) helper ---
        registerUserByRole.RegisterStaff(email, employeeId, DEFAULT_PASSWORD, staffRole,
                firstName, lastName, middleName, dob, gender, joiningDate, jobTitle,
                department, StaffType.LIBRARIAN, row);
    }
}