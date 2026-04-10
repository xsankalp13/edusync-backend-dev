package com.project.edusync.iam.service.impl;

import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.model.entity.Subject;
import com.project.edusync.adm.model.entity.Timeslot;
import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.adm.repository.SubjectRepository;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.exception.iam.UserAlreadyExistsException;
import com.project.edusync.common.service.EmailService;
import com.project.edusync.em.model.repository.StudentMarkRepository;
import com.project.edusync.iam.model.dto.*;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.iam.service.UserManagementService;
import com.project.edusync.uis.mapper.*;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.StudentGuardianRelationship;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.details.LibrarianDetails;
import com.project.edusync.uis.model.entity.details.PrincipalDetails;
import com.project.edusync.uis.model.entity.details.StudentDemographics;
import com.project.edusync.uis.model.entity.details.TeacherDetails;
import com.project.edusync.uis.model.entity.medical.StudentMedicalRecord;
import com.project.edusync.uis.model.enums.StaffType;
import com.project.edusync.uis.model.dto.profile.ComprehensiveUserProfileResponseDTO;
import com.project.edusync.uis.model.dto.profile.GuardianProfileDTO;
import com.project.edusync.uis.model.dto.profile.LinkedStudentDTO;
import com.project.edusync.uis.model.dto.profile.StaffKpiMetricsDTO;
import com.project.edusync.uis.model.dto.profile.StudentKpiMetricsDTO;
import com.project.edusync.uis.model.dto.profile.StudentGuardianDTO;
import com.project.edusync.uis.repository.*;
import com.project.edusync.uis.repository.details.LibrarianDetailsRepository;
import com.project.edusync.uis.repository.details.PrincipalDetailsRepository;
import com.project.edusync.uis.repository.details.StudentDemographicsRepository;
import com.project.edusync.uis.repository.details.TeacherDetailsRepository;
import com.project.edusync.uis.repository.medical.StudentMedicalRecordRepository;
import com.project.edusync.uis.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of UserManagementService.
 * <p>
 * This class handles the transactional creation of users across multiple tables.
 * It strictly uses the custom exception hierarchy (EdusyncException) for any failures.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementServiceImpl implements UserManagementService {

    // --- Core Identity ---
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SectionRepository sectionRepository;

    // --- UIS Core ---
    private final UserProfileRepository userProfileRepository;
    private final JsonMapper jsonMapper;

    // --- Role Entities ---
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRelationshipRepository studentGuardianRelationshipRepository;
    private final com.project.edusync.hrms.repository.StaffDesignationRepository staffDesignationRepository;

    // --- Extension Repositories ---
    private final TeacherDetailsRepository teacherDetailsRepository;
    private final SubjectRepository subjectRepository;
    private final PrincipalDetailsRepository principalDetailsRepository;
    private final LibrarianDetailsRepository librarianDetailsRepository;
    private final StudentDemographicsRepository studentDemographicsRepository;
    private final StudentMedicalRecordRepository studentMedicalRecordRepository;
    private final StudentDailyAttendanceRepository studentDailyAttendanceRepository;
    private final StaffDailyAttendanceRepository staffDailyAttendanceRepository;
    private final StudentMarkRepository studentMarkRepository;
    private final ScheduleRepository scheduleRepository;

    // --- Mappers ---
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final StudentMapper studentMapper;
    private final StaffMapper staffMapper;
    private final TeacherMapper teacherMapper;
    private final PrincipalMapper principalMapper;
    private final LibrarianMapper librarianMapper;
    private final GuardianMapper guardianMapper;
    private final ProfileService profileService;

    // =================================================================================
    // 1. SCHOOL ADMIN
    // =================================================================================
    @Override
    @Transactional
    public User createSchoolAdmin(CreateUserRequestDTO request) {
        log.info("Process started: Creating School Admin with username: {}", request.getUsername());
        User user = createUserWithRole(request, "SCHOOL_ADMIN");
        log.info("Success: School Admin created. User ID: {}", user.getId());
        return user;
    }

    // =================================================================================
    // 2. STUDENT (Comprehensive)
    // =================================================================================
    @Override
    @Transactional
    public User createStudent(CreateStudentRequestDTO request) {
        log.info("Process started: Enrolling new Student: {}", request.getUsername());

        // 1. Create Base User & Profile (Identity Layer)
        User user = createUserWithRole(request, "STUDENT");

        // 2. Fetch Profile to link relationships
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new EdusyncException("System Error: Profile creation verification failed for user " + user.getUsername(), HttpStatus.INTERNAL_SERVER_ERROR));

        // 2. Fetch the Section
        Section section = sectionRepository.findByUuid(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section", "uuid", request.getSectionId()));
        // 3. Create Core Student Entity
        Student student = studentMapper.toStudentEntity(request);
        student.setUserProfile(profile);
        student.setSection(section);

        // Auto-generate ID if not provided (Safety net)
        if (student.getEnrollmentNumber() == null) {
            String generatedId = generateEnrollmentId();
            log.debug("Auto-generating Enrollment Number: {}", generatedId);
            student.setEnrollmentNumber(generatedId);
        }
        // Check for duplicate enrollment number
        if (studentRepository.existsByEnrollmentNumber(student.getEnrollmentNumber())) {
            throw new UserAlreadyExistsException("Student with enrollment number " + student.getEnrollmentNumber() + " already exists.");
        }

        if (student.getEnrollmentDate() == null) {
            student.setEnrollmentDate(LocalDate.now());
        }

        Student savedStudent = studentRepository.save(student);
        log.debug("Core Student record saved. ID: {}", savedStudent.getId());
        log.info("Success: Comprehensive Student enrollment complete. Enrollment #: {}", savedStudent.getEnrollmentNumber());
        return user;
    }

    // =================================================================================
    // 3. STAFF
    // =================================================================================

    @Override
    @Transactional
    public User createTeacher(CreateTeacherRequestDTO request) {
        log.info("Process started: Hiring Teacher: {}", request.getUsername());

        // 1. Create User -> Profile -> Staff
        Staff staff = createBaseStaff(request);

        // 2. Create Teacher Details Extension
        TeacherDetails details = teacherMapper.toEntity(request);
        details.setStaff(staff); // Link via @MapsId
        details.setTeachableSubjects(resolveTeachableSubjects(request.getTeachableSubjectIds()));

        teacherDetailsRepository.save(details);
        log.info("Success: Teacher created with ID: {}", staff.getId());

        return staff.getUserProfile().getUser();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"editorContext", "availableTeachers"}, allEntries = true)
    public void bulkAssignSubjectToTeachers(BulkTeacherSubjectAssignmentRequestDTO request) {
        log.info("Process started: Bulk teacher subject assignment. subjectId={}, teacherCount={}",
                request.getSubjectId(), request.getTeacherIds().size());

        Subject subject = subjectRepository.findActiveById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "uuid", request.getSubjectId()));

        List<TeacherDetails> teachersToUpdate = new ArrayList<>();
        for (Long teacherId : request.getTeacherIds()) {
            TeacherDetails teacherDetails = teacherDetailsRepository.findActiveById(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

            teacherDetails.getTeachableSubjects().add(subject);
            teachersToUpdate.add(teacherDetails);
        }

        teacherDetailsRepository.saveAll(teachersToUpdate);
        log.info("Success: Bulk teacher subject assignment completed. subjectId={}, updatedTeachers={}",
                request.getSubjectId(), teachersToUpdate.size());
    }

    @Override
    @Transactional
    public User createPrincipal(CreatePrincipalRequestDTO request) {
        log.info("Process started: Appointing Principal: {}", request.getUsername());

        Staff staff = createBaseStaff(request);

        PrincipalDetails details = principalMapper.toEntity(request);
        details.setStaff(staff);

        principalDetailsRepository.save(details);
        log.info("Success: Principal created with ID: {}", staff.getId());

        return staff.getUserProfile().getUser();
    }

    @Override
    @Transactional
    public User createLibrarian(CreateLibrarianRequestDTO request) {
        log.info("Process started: Hiring Librarian: {}", request.getUsername());

        Staff staff = createBaseStaff(request);

        LibrarianDetails details = librarianMapper.toEntity(request);
        details.setStaff(staff);

        librarianDetailsRepository.save(details);
        log.info("Success: Librarian created with ID: {}", staff.getId());

        return staff.getUserProfile().getUser();
    }

    @Override
    @Transactional
    public User createGuardian(UUID studentId, CreateGuardianRequestDTO request) {
        log.info("Process started: Creating Guardian [{}] for Student UUID: {}", request.getUsername(), studentId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        User user = createUserWithRole(request, "GUARDIAN");

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new EdusyncException("System Error: Profile creation verification failed.", HttpStatus.INTERNAL_SERVER_ERROR));

        Guardian guardian = new Guardian();
        guardian.setUserProfile(profile);
        guardian.setPhoneNumber(request.getPhoneNumber());
        guardian.setOccupation(request.getOccupation());
        guardian.setEmployer(request.getEmployer());
        guardian.setActive(true);
        Guardian savedGuardian = guardianRepository.save(guardian);

        StudentGuardianRelationship relation = new StudentGuardianRelationship();
        relation.setStudent(student);
        relation.setGuardian(savedGuardian);
        relation.setRelationshipType(request.getRelationshipType());
        relation.setPrimaryContact(request.isPrimaryContact());
        relation.setCanPickup(request.isCanPickup());
        relation.setFinancialContact(request.isFinancialContact());
        relation.setCanViewGrades(request.isCanViewGrades());
        studentGuardianRelationshipRepository.save(relation);

        log.info("Success: Guardian created and linked. guardianUuid={}, studentUuid={}", savedGuardian.getUuid(), studentId);
        return user;
    }

    @Override
    @Transactional
    public void linkExistingGuardian(UUID studentId, LinkGuardianRequestDTO request) {
        log.info("Process started: Linking existing Guardian. studentUuid={}, guardianUuid={}", studentId, request.getGuardianId());

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));
        Guardian guardian = guardianRepository.findByUuid(request.getGuardianId())
                .orElseThrow(() -> new ResourceNotFoundException("Guardian", "uuid", request.getGuardianId()));

        if (studentGuardianRelationshipRepository.existsByStudentAndGuardian(student, guardian)) {
            throw new EdusyncException("Guardian is already linked to this student.", HttpStatus.CONFLICT);
        }

        StudentGuardianRelationship relation = new StudentGuardianRelationship();
        relation.setStudent(student);
        relation.setGuardian(guardian);
        relation.setRelationshipType(request.getRelationshipType());
        relation.setPrimaryContact(request.isPrimaryContact());
        relation.setCanPickup(request.isCanPickup());
        relation.setFinancialContact(request.isFinancialContact());
        relation.setCanViewGrades(request.isCanViewGrades());
        studentGuardianRelationshipRepository.save(relation);

        log.info("Success: Existing guardian linked. studentUuid={}, guardianUuid={}", studentId, request.getGuardianId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentGuardianDTO> getGuardiansByStudent(UUID studentId) {
        log.info("Process started: Fetching guardians for studentUuid={}", studentId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        List<StudentGuardianDTO> response = new ArrayList<>();
        List<StudentGuardianRelationship> relations = studentGuardianRelationshipRepository.findByStudent(student);
        for (StudentGuardianRelationship relation : relations) {
            Guardian guardian = relation.getGuardian();
            if (guardian == null) {
                continue;
            }

            StudentGuardianDTO dto = new StudentGuardianDTO();
            dto.setGuardianUuid(guardian.getUuid());
            UserProfile guardianProfile = guardian.getUserProfile();
            if (guardianProfile != null) {
                String firstName = guardianProfile.getFirstName() != null ? guardianProfile.getFirstName() : "";
                String lastName = guardianProfile.getLastName() != null ? guardianProfile.getLastName() : "";
                dto.setName((firstName + " " + lastName).trim());
                dto.setProfileUrl(guardianProfile.getProfileUrl());
                dto.setDateOfBirth(guardianProfile.getDateOfBirth());
            }
            dto.setRelation(relation.getRelationshipType());
            dto.setPhoneNumber(guardian.getPhoneNumber());
            dto.setOccupation(guardian.getOccupation());
            dto.setEmployer(guardian.getEmployer());
            dto.setPrimaryContact(relation.isPrimaryContact());
            dto.setCanPickup(relation.isCanPickup());
            dto.setFinancialContact(relation.isFinancialContact());
            dto.setCanViewGrades(relation.isCanViewGrades());
            dto.setActive(guardian.isActive());
            response.add(dto);
        }

        log.info("Success: Guardians fetched for studentUuid={}, count={}", studentId, response.size());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkedStudentDTO> getLinkedStudentsByGuardian(UUID guardianId) {
        log.info("Process started: Fetching linked students for guardianUuid={}", guardianId);

        Guardian guardian = guardianRepository.findByUuid(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian", "uuid", guardianId));

        List<StudentGuardianRelationship> relations = studentGuardianRelationshipRepository.findByGuardian(guardian);
        List<LinkedStudentDTO> response = new ArrayList<>();
        for (StudentGuardianRelationship relation : relations) {
            response.add(guardianMapper.toLinkedStudentDto(relation));
        }

        log.info("Success: Linked students fetched for guardianUuid={}, count={}", guardianId, response.size());
        return response;
    }

    // =================================================================================
    // INTERNAL HELPERS
    // =================================================================================

    /**
     * Shared logic to create the Identity (User), Profile, and Base Staff record.
     * Prevents code duplication across different staff types.
     */
    private Staff createBaseStaff(BaseStaffRequestDTO request) {
        // A. Identity & Profile
        User user = createUserWithRole(request, request.getStaffType().name());

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new EdusyncException("System Error: Profile creation verification failed.", HttpStatus.INTERNAL_SERVER_ERROR));

        // B. Base Staff Entity
        Staff staff = staffMapper.toEntity(request);
        staff.setUserProfile(profile);
        if (!StringUtils.hasText(staff.getEmployeeId())) {
            staff.setEmployeeId(request.getUsername());
        }

        com.project.edusync.hrms.model.entity.StaffDesignation designation = staffDesignationRepository.findByDesignationNameIgnoreCase(request.getJobTitle())
                .orElseGet(() -> {
                    com.project.edusync.hrms.model.entity.StaffDesignation newDesig = new com.project.edusync.hrms.model.entity.StaffDesignation();
                    newDesig.setDesignationName(request.getJobTitle());
                    String safeCode = request.getJobTitle().toUpperCase().replaceAll("[^A-Z0-9]", "_");
                    if (safeCode.length() > 15) {
                         safeCode = safeCode.substring(0, 15);
                    }
                    safeCode = safeCode + "_" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                    newDesig.setDesignationCode(safeCode);
                    newDesig.setCategory(request.getCategory());
                    newDesig.setActive(true);
                    newDesig.setSortOrder(99);
                    return staffDesignationRepository.save(newDesig);
                });
        staff.setDesignation(designation);

        // Note: Staff ID is generated here
        return staffRepository.save(staff);
    }

    /**
     * Core logic to create the User and UserProfile.
     * Handles Validation, Password Hashing, and Role Assignment.
     */
    private User createUserWithRole(CreateUserRequestDTO request, String roleName) {
        // 1. Check for Duplicate Username
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed: Username '{}' already exists.", request.getUsername());
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken.");
        }
        // 1b. Check for Duplicate Email
        // Assuming your User repository has a method for this, or you handle it via DataIntegrityViolationException
        // We will do a manual check for cleaner error messages
         if (userRepository.existsByEmail(request.getEmail())) {
             throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' already exists.");
         }

        // 2. Validate Role Existence
        Role role = roleRepository.findByName("ROLE_"+roleName)
                .orElseThrow(() -> {
                    log.error("Configuration Error: Role '{}' not found in database.", roleName);
                    return new ResourceNotFoundException("Role", "name", roleName);
                });

        // 3. Create User Entity
        User user = userMapper.toEntity(request);

        // Secure Password Generation
        log.info("Generating password for new user. Username: {}, Role: {}", request.getUsername(), roleName);
        log.info("Password : " + request.getInitialPassword());
        String rawPassword = request.getInitialPassword() != null ?
                request.getInitialPassword() :
                request.getUsername();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        user.setRoles(Collections.singleton(role));

        User savedUser = userRepository.save(user);
        log.debug("User identity created. ID: {}", savedUser.getId());

        // 4. Create Profile Entity
        UserProfile profile = userProfileMapper.toEntity(request);
        profile.setUser(savedUser); // Foreign Key Link

        userProfileRepository.save(profile);
        log.debug("User profile created. Profile ID: {}", profile.getId());

        // 5. Send Welcome Email (Async)
        // emailService.sendWelcomeEmail(savedUser, rawPassword);

        return savedUser;
    }

    private String generateEnrollmentId() {
        return "STU-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Override
    @Transactional
    public User updateStudent(UUID studentId, UpdateStudentRequestDTO request) {
        log.info("Process started: Updating Student. UUID: {}", studentId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        UserProfile profile = student.getUserProfile();
        User user = profile.getUser();

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' already exists.");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getEnrollmentNumber())
                && !request.getEnrollmentNumber().equals(student.getEnrollmentNumber())) {
            if (studentRepository.existsByEnrollmentNumber(request.getEnrollmentNumber())) {
                throw new UserAlreadyExistsException("Student with enrollment number " + request.getEnrollmentNumber() + " already exists.");
            }
            userRepository.findByUsername(request.getEnrollmentNumber()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new UserAlreadyExistsException("Username '" + request.getEnrollmentNumber() + "' already exists.");
                }
            });
            student.setEnrollmentNumber(request.getEnrollmentNumber());
            user.setUsername(request.getEnrollmentNumber());
        }

        if (StringUtils.hasText(request.getFirstName())) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) {
            profile.setMiddleName(request.getMiddleName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            profile.setLastName(request.getLastName());
        }
        if (request.getPreferredName() != null) {
            profile.setPreferredName(request.getPreferredName());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        if (request.getRollNo() != null) {
            student.setRollNo(request.getRollNo());
        }
        if (request.getEnrollmentDate() != null) {
            student.setEnrollmentDate(request.getEnrollmentDate());
        }
        if (request.getSectionId() != null) {
            Section section = sectionRepository.findByUuid(request.getSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section", "uuid", request.getSectionId()));
            student.setSection(section);
        }

        userRepository.save(user);
        userProfileRepository.save(profile);
        studentRepository.save(student);

        log.info("Success: Student updated. UUID: {}", studentId);
        return user;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"editorContext", "availableTeachers"}, allEntries = true)
    public User updateStaff(UUID staffId, UpdateStaffRequestDTO request) {
        log.info("Process started: Updating Staff. UUID: {}", staffId);

        Staff staff = staffRepository.findByUuid(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffId));

        UserProfile profile = staff.getUserProfile();
        User user = profile.getUser();

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' already exists.");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getEmployeeId()) && !request.getEmployeeId().equals(staff.getEmployeeId())) {
            if (staffRepository.existsByEmployeeId(request.getEmployeeId())) {
                throw new UserAlreadyExistsException("Staff with employee ID " + request.getEmployeeId() + " already exists.");
            }
            userRepository.findByUsername(request.getEmployeeId()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new UserAlreadyExistsException("Username '" + request.getEmployeeId() + "' already exists.");
                }
            });
            staff.setEmployeeId(request.getEmployeeId());
            user.setUsername(request.getEmployeeId());
        }

        if (StringUtils.hasText(request.getFirstName())) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) {
            profile.setMiddleName(request.getMiddleName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            profile.setLastName(request.getLastName());
        }
        if (request.getPreferredName() != null) {
            profile.setPreferredName(request.getPreferredName());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        if (StringUtils.hasText(request.getJobTitle())) {
            staff.setJobTitle(request.getJobTitle());
        }
        if (request.getHireDate() != null) {
            staff.setHireDate(request.getHireDate());
        }
        if (request.getOfficeLocation() != null) {
            staff.setOfficeLocation(request.getOfficeLocation());
        }
        if (request.getDepartment() != null) {
            staff.setDepartment(request.getDepartment());
        }
        if (request.getStaffType() != null) {
            staff.setStaffType(request.getStaffType());
        }

        if (request.getTeachableSubjectIds() != null) {
            StaffType effectiveStaffType = request.getStaffType() != null ? request.getStaffType() : staff.getStaffType();
            if (!StaffType.TEACHER.equals(effectiveStaffType)) {
                throw new EdusyncException("teachableSubjectIds is only supported for teacher staff records.", HttpStatus.BAD_REQUEST);
            }

            TeacherDetails teacherDetails = teacherDetailsRepository.findById(staff.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("TeacherDetails", "staffId", staff.getId()));
            teacherDetails.setTeachableSubjects(resolveTeachableSubjects(request.getTeachableSubjectIds()));
            teacherDetailsRepository.save(teacherDetails);
        }

        userRepository.save(user);
        userProfileRepository.save(profile);
        staffRepository.save(staff);

        log.info("Success: Staff updated. UUID: {}", staffId);
        return user;
    }

    private Set<Subject> resolveTeachableSubjects(List<UUID> teachableSubjectIds) {
        if (teachableSubjectIds == null || teachableSubjectIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Subject> subjects = new HashSet<>();
        for (UUID subjectId : teachableSubjectIds) {
            Subject subject = subjectRepository.findActiveById(subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", "uuid", subjectId));
            subjects.add(subject);
        }
        return subjects;
    }

    @Override
    @Transactional
    public User updateGuardian(UUID studentId, UUID guardianId, UpdateGuardianRequestDTO request) {
        log.info("Process started: Updating Guardian. studentUuid={}, guardianUuid={}", studentId, guardianId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        Guardian guardian = guardianRepository.findByUuid(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian", "uuid", guardianId));

        StudentGuardianRelationship relation = studentGuardianRelationshipRepository.findByStudentAndGuardian(student, guardian)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGuardianRelationship", "student+guardian", studentId + "+" + guardianId));

        UserProfile profile = guardian.getUserProfile();
        User user = profile.getUser();

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' already exists.");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getFirstName())) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) {
            profile.setMiddleName(request.getMiddleName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            profile.setLastName(request.getLastName());
        }
        if (request.getPreferredName() != null) {
            profile.setPreferredName(request.getPreferredName());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        if (StringUtils.hasText(request.getPhoneNumber())) {
            guardian.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getOccupation() != null) {
            guardian.setOccupation(request.getOccupation());
        }
        if (request.getEmployer() != null) {
            guardian.setEmployer(request.getEmployer());
        }

        if (StringUtils.hasText(request.getRelationshipType())) {
            relation.setRelationshipType(request.getRelationshipType());
        }
        if (request.getPrimaryContact() != null) {
            relation.setPrimaryContact(request.getPrimaryContact());
        }
        if (request.getCanPickup() != null) {
            relation.setCanPickup(request.getCanPickup());
        }
        if (request.getFinancialContact() != null) {
            relation.setFinancialContact(request.getFinancialContact());
        }
        if (request.getCanViewGrades() != null) {
            relation.setCanViewGrades(request.getCanViewGrades());
        }

        userRepository.save(user);
        userProfileRepository.save(profile);
        guardianRepository.save(guardian);
        studentGuardianRelationshipRepository.save(relation);

        log.info("Success: Guardian updated. studentUuid={}, guardianUuid={}", studentId, guardianId);
        return user;
    }

    @Override
    @Transactional
    public void softDeleteStudent(UUID studentId) {
        log.info("Process started: Soft deleting Student. UUID: {}", studentId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));
        User user = student.getUserProfile().getUser();
        user.setActive(false);
        student.setActive(false);
        userRepository.save(user);
        studentRepository.save(student);
        log.info("Student user deactivated successfully. studentUuid={}, userId={}", studentId, user.getId());
    }

    @Override
    @Transactional
    public void softDeleteStaff(UUID staffId) {
        log.info("Process started: Soft deleting Staff. UUID: {}", staffId);

        Staff staff = staffRepository.findByUuid(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffId));
        User user = staff.getUserProfile().getUser();
        user.setActive(false);
        staff.setActive(false);
        userRepository.save(user);
        staffRepository.save(staff);
        log.info("Staff user deactivated successfully. staffUuid={}, userId={}", staffId, user.getId());
    }

    @Override
    @Transactional
    public void softDeleteGuardian(UUID studentId, UUID guardianId) {
        log.info("Process started: Soft deleting Guardian. studentUuid={}, guardianUuid={}", studentId, guardianId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));
        Guardian guardian = guardianRepository.findByUuid(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian", "uuid", guardianId));

        studentGuardianRelationshipRepository.findByStudentAndGuardian(student, guardian)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGuardianRelationship", "student+guardian", studentId + "+" + guardianId));

        User user = guardian.getUserProfile().getUser();
        user.setActive(false);
        guardian.setActive(false);
        userRepository.save(user);
        guardianRepository.save(guardian);
        log.info("Guardian deactivated successfully. guardianUuid={}, userId={}", guardianId, user.getId());
    }

    @Override
    @Transactional
    public void unlinkGuardian(UUID studentId, UUID guardianId) {
        log.info("Process started: Unlinking guardian. studentUuid={}, guardianUuid={}", studentId, guardianId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));
        Guardian guardian = guardianRepository.findByUuid(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian", "uuid", guardianId));

        studentGuardianRelationshipRepository.findByStudentAndGuardian(student, guardian)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGuardianRelationship", "student+guardian", studentId + "+" + guardianId));

        studentGuardianRelationshipRepository.deleteByStudentAndGuardian(student, guardian);
        log.info("Success: Guardian unlinked from student. studentUuid={}, guardianUuid={}", studentId, guardianId);
    }

    @Override
    @Transactional
    public void setStudentUserActivation(UUID studentId, boolean active) {
        log.info("Process started: Setting Student user activation. studentUuid={}, active={}", studentId, active);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        User user = student.getUserProfile().getUser();
        user.setActive(active);
        student.setActive(active);
        userRepository.save(user);
        studentRepository.save(student);

        log.info("Success: Student user activation updated. studentUuid={}, userId={}, active={}",
                studentId, user.getId(), active);
    }

    @Override
    @Transactional
    public void setStaffUserActivation(UUID staffId, boolean active) {
        log.info("Process started: Setting Staff user activation. staffUuid={}, active={}", staffId, active);

        Staff staff = staffRepository.findByUuid(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffId));

        User user = staff.getUserProfile().getUser();
        user.setActive(active);
        staff.setActive(active);
        userRepository.save(user);
        staffRepository.save(staff);

        log.info("Success: Staff user activation updated. staffUuid={}, userId={}, active={}",
                staffId, user.getId(), active);
    }

    @Override
    @Transactional
    public void setGuardianUserActivation(UUID studentId, UUID guardianId, boolean active) {
        log.info("Process started: Setting Guardian activation. studentUuid={}, guardianUuid={}, active={}",
                studentId, guardianId, active);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));
        Guardian guardian = guardianRepository.findByUuid(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian", "uuid", guardianId));

        studentGuardianRelationshipRepository.findByStudentAndGuardian(student, guardian)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGuardianRelationship", "student+guardian", studentId + "+" + guardianId));

        User user = guardian.getUserProfile().getUser();
        user.setActive(active);
        guardian.setActive(active);
        userRepository.save(user);
        guardianRepository.save(guardian);

        log.info("Success: Guardian activation updated. guardianUuid={}, userId={}, active={}",
                guardianId, user.getId(), active);
    }

    @Override
    @Transactional(readOnly = true)
    public ComprehensiveUserProfileResponseDTO getStudentFullDetails(UUID studentId) {
        log.info("Process started: Fetching full Student details. UUID: {}", studentId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        Long userId = student.getUserProfile().getUser().getId();
        ComprehensiveUserProfileResponseDTO response = profileService.getProfileByUserId(userId);

        log.info("Success: Full Student details fetched. Student UUID: {}, User ID: {}", studentId, userId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ComprehensiveUserProfileResponseDTO getStaffFullDetails(UUID staffId) {
        log.info("Process started: Fetching full Staff details. UUID: {}", staffId);

        Staff staff = staffRepository.findByUuid(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffId));

        Long userId = staff.getUserProfile().getUser().getId();
        ComprehensiveUserProfileResponseDTO response = profileService.getProfileByUserId(userId);

        log.info("Success: Full Staff details fetched. Staff UUID: {}, User ID: {}", staffId, userId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentKpiMetricsDTO getStudentKpiMetrics(UUID studentId) {
        log.info("Process started: Fetching Student KPI metrics. UUID: {}", studentId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        BigDecimal attendanceRate = calculateStudentAttendancePercentage(student.getId());
        BigDecimal gpa = calculateStudentGpaOnFourScale(student.getId());

        StudentKpiMetricsDTO dto = new StudentKpiMetricsDTO();
        dto.setStudentId(student.getId());
        dto.setAttendanceRatePercentage(attendanceRate);
        dto.setGpa(gpa);
        dto.setAcademicStanding(resolveAcademicStanding(gpa));
        dto.setCurrentGrade(student.getSection().getAcademicClass().getName());
        dto.setCurrentSection(student.getSection().getSectionName());
        dto.setOpenDisciplinaryIncidents(0L);

        log.info("Success: Student KPI metrics computed. studentUuid={}, studentId={}, attendance={}, gpa={}",
                studentId, student.getId(), attendanceRate, gpa);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffKpiMetricsDTO getStaffKpiMetrics(UUID staffId) {
        log.info("Process started: Fetching Staff KPI metrics. UUID: {}", staffId);

        Staff staff = staffRepository.findByUuid(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffId));

        BigDecimal attendanceRate = calculateStaffAttendancePercentage(staff.getId());
        Long totalClassesAssigned = 0L;
        Integer weeklyHoursAssigned = 0;

        if (StaffType.TEACHER.equals(staff.getStaffType())) {
            TeacherDetails teacherDetails = teacherDetailsRepository.findByStaff_Id(staff.getId())
                    .orElse(null);

            if (teacherDetails != null) {
                totalClassesAssigned = scheduleRepository.countDistinctActiveSectionsByTeacherId(teacherDetails.getId());
                weeklyHoursAssigned = calculateWeeklyHoursAssigned(teacherDetails.getId());
            }
        }

        StaffKpiMetricsDTO dto = new StaffKpiMetricsDTO();
        dto.setStaffId(staff.getId());
        dto.setPerformanceRating(null);
        dto.setTotalClassesAssigned(totalClassesAssigned);
        dto.setWeeklyHoursAssigned(weeklyHoursAssigned);
        dto.setAttendanceRatePercentage(attendanceRate);

        log.info("Success: Staff KPI metrics computed. staffUuid={}, staffId={}, attendance={}, classesAssigned={}, weeklyHours={}",
                staffId, staff.getId(), attendanceRate, totalClassesAssigned, weeklyHoursAssigned);
        return dto;
    }

    private BigDecimal calculateStudentAttendancePercentage(Long studentId) {
        long total = studentDailyAttendanceRepository.countByStudentId(studentId);
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        long present = studentDailyAttendanceRepository.countPresentByStudentId(studentId);
        return BigDecimal.valueOf(present)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStaffAttendancePercentage(Long staffId) {
        long total = staffDailyAttendanceRepository.countByStaffId(staffId);
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        long present = staffDailyAttendanceRepository.countPresentByStaffId(staffId);
        return BigDecimal.valueOf(present)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStudentGpaOnFourScale(Long studentId) {
        List<StudentMarkRepository.PerformanceTrendView> trend = studentMarkRepository.findPerformanceTrendByStudentId(studentId);
        if (trend.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal latestTenScale = trend.get(trend.size() - 1).getScore();
        if (latestTenScale == null) {
            return BigDecimal.ZERO;
        }

        return latestTenScale
                .multiply(BigDecimal.valueOf(0.4))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveAcademicStanding(BigDecimal gpaOnFourScale) {
        if (gpaOnFourScale == null) {
            return "AT_RISK";
        }
        if (gpaOnFourScale.compareTo(BigDecimal.valueOf(3.7)) >= 0) {
            return "EXCELLENT";
        }
        if (gpaOnFourScale.compareTo(BigDecimal.valueOf(3.0)) >= 0) {
            return "GOOD";
        }
        if (gpaOnFourScale.compareTo(BigDecimal.valueOf(2.0)) >= 0) {
            return "AVERAGE";
        }
        return "AT_RISK";
    }

    private Integer calculateWeeklyHoursAssigned(Long teacherId) {
        List<Timeslot> timeslots = scheduleRepository.findDistinctActiveTimeslotsByTeacherId(teacherId);
        if (timeslots.isEmpty()) {
            return 0;
        }

        int minutes = timeslots.stream()
                .mapToInt(ts -> {
                    if (ts.getStartTime() == null || ts.getEndTime() == null) {
                        return 0;
                    }
                    return (int) java.time.Duration.between(ts.getStartTime(), ts.getEndTime()).toMinutes();
                })
                .sum();

        return (int) Math.round(minutes / 60.0);
    }
}
