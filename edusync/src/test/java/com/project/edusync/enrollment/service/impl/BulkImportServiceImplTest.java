package com.project.edusync.enrollment.service.impl;

import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.enrollment.util.CsvValidationHelper;
import com.project.edusync.enrollment.util.RegisterUserByRole; // <-- NEW MOCK
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.enums.Department;
import com.project.edusync.uis.model.enums.Gender;
import com.project.edusync.uis.model.enums.StaffType;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder; // Keep this

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Service-layer unit test for BulkImportServiceImpl.
 * This test isolates the service and mocks all external dependencies,
 * including the RegisterUserByRole helper, to match the new architecture.
 */
@ExtendWith(MockitoExtension.class)
class BulkImportServiceImplTest {

    // --- Mock all dependencies ---
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private CsvValidationHelper validationHelper;
    @Mock
    private PasswordEncoder passwordEncoder; // This is still used in processStaffRow

    // --- NEW: Mock the helper class ---
    @Mock
    private RegisterUserByRole registerUserByRole;

    // --- REMOVED: Mock for UserProfileRepository, as service no longer calls it
    // @Mock
    // private UserProfileRepository userProfileRepository;

    @InjectMocks
    private BulkImportServiceImpl bulkImportService;

    // --- NEW: ArgumentCaptors for helper method parameters ---
    @Captor
    private ArgumentCaptor<String> stringCaptor;
    @Captor
    private ArgumentCaptor<Integer> intCaptor;
    @Captor
    private ArgumentCaptor<LocalDate> dateCaptor;
    @Captor
    private ArgumentCaptor<Role> roleCaptor;
    @Captor
    private ArgumentCaptor<Gender> genderCaptor;
    @Captor
    private ArgumentCaptor<Section> sectionCaptor;
    @Captor
    private ArgumentCaptor<Department> departmentCaptor;
    @Captor
    private ArgumentCaptor<StaffType> staffTypeCaptor;
    @Captor
    private ArgumentCaptor<String[]> rowCaptor;

    // --- Test Data ---
    private String[] validStudentRow;
    private String[] validStaffRow;
    private Role mockStudentRole;
    private Role mockStaffRole;
    private Section mockSection;
    private Map<String, Role> mockRoleCache;
    private Map<String, Section> mockSectionCache;

    @BeforeEach
    void setUp() {
        // --- UPDATED: 11-column row based on your latest code ---
        validStudentRow = new String[]{
                "John", "Doe", "M", "john.doe@example.com", "2005-01-15",
                "101", "MALE", "S12345", "2023-09-01", "Class 10", "A"
        };
        // Indices:
        // 0: firstName, 1: lastName, 2: middleName, 3: email, 4: dob,
        // 5: rollNo, 6: gender, 7: enrollmentNumber, 8: enrollmentDate,
        // 9: className, 10: sectionName

        // --- UPDATED: 11+ column row for staff (15 columns) ---
        validStaffRow = new String[]{
                "Jane", "Smith", "", "jane.smith@example.com", "1990-05-20", "FEMALE",
                "T98765", "2015-08-15", "Math Teacher", "ACADEMICS", "TEACHER",
                "CertA,CertB", "Math,Physics", "10", "MASTERS"
        };

        // Mock entities that are *fetched*
        mockStudentRole = new Role();
        mockStudentRole.setName("ROLE_STUDENT");

        mockStaffRole = new Role();
        mockStaffRole.setName("ROLE_TEACHER");

        mockSection = new Section();
        mockSection.setSectionName("A");
        // We create a mock AcademicClass just for the cache key creation
        // Note: We don't need to mock the repository for this
        com.project.edusync.adm.model.entity.AcademicClass mockClass = new com.project.edusync.adm.model.entity.AcademicClass();
        mockClass.setName("Class 10");
        mockSection.setAcademicClass(mockClass);


        // --- NEW: Mock the caches that importUsers would have built ---
        mockRoleCache = Map.of(
                "ROLE_STUDENT", mockStudentRole,
                "ROLE_TEACHER", mockStaffRole
        );
        mockSectionCache = Collections.singletonMap("Class 10:A", mockSection);
    }

    /**
     * Mocks all CsvValidationHelper calls for the 11-column student row.
     */
    private void mockStudentHelperSuccess() throws Exception {
        when(validationHelper.validateString(validStudentRow[0], "firstName")).thenReturn("John");
        when(validationHelper.validateString(validStudentRow[1], "lastName")).thenReturn("Doe");
        // middleName (row[2]) is optional, not validated
        when(validationHelper.validateEmail(validStudentRow[3])).thenReturn("john.doe@example.com");
        when(validationHelper.parseDate(validStudentRow[4], "dateOfBirth")).thenReturn(LocalDate.of(2005, 1, 15));

        // --- UPDATED INDICES ---
        when(validationHelper.validateString(validStudentRow[5], "rollNo")).thenReturn("101"); // for Integer.parseInt
        when(validationHelper.parseEnum(Gender.class, validStudentRow[6], "gender")).thenReturn(Gender.MALE);
        when(validationHelper.validateString(validStudentRow[7], "enrollmentNumber")).thenReturn("S12345");
        when(validationHelper.parseDate(validStudentRow[8], "enrollmentDate")).thenReturn(LocalDate.of(2023, 9, 1));
        when(validationHelper.validateString(validStudentRow[9], "className")).thenReturn("Class 10");
        when(validationHelper.validateString(validStudentRow[10], "sectionName")).thenReturn("A");
    }

    @Test
    void processStudentRow_success() throws Exception {
        // --- 1. Arrange (Mock all external calls) ---
        mockStudentHelperSuccess();

        // Mock business logic checks (no duplicates)
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(studentRepository.existsByEnrollmentNumber("S12345")).thenReturn(false);

        // Mock the caches that are passed in
        // (We don't need to mock roleRepository.findByName, etc. anymore)

        // --- 2. Act (Call the method with the mock caches) ---
        bulkImportService.processStudentRow(validStudentRow, mockRoleCache, mockSectionCache);

        // --- 3. Assert (Verify helper was called with correct data) ---

        // Verify the helper was called exactly once with all 12 arguments
        verify(registerUserByRole, times(1)).RegisterStudent(
                stringCaptor.capture(), // email
                stringCaptor.capture(), // enrollmentNumber
                stringCaptor.capture(), // DEFAULT_PASSWORD
                roleCaptor.capture(),   // studentRole
                stringCaptor.capture(), // firstName
                stringCaptor.capture(), // lastName
                stringCaptor.capture(), // middleName
                dateCaptor.capture(),   // dob
                genderCaptor.capture(), // gender
                dateCaptor.capture(),   // enrollmentDate
                sectionCaptor.capture(),// section
                intCaptor.capture()     // rollNo
        );

        // Verify the parsed values were passed correctly
        assertEquals("john.doe@example.com", stringCaptor.getAllValues().get(0));
        assertEquals("S12345", stringCaptor.getAllValues().get(1));
        assertEquals("John", stringCaptor.getAllValues().get(3));
        assertEquals("Doe", stringCaptor.getAllValues().get(4));
        assertEquals("M", stringCaptor.getAllValues().get(5)); // middleName
        assertEquals(mockStudentRole, roleCaptor.getValue());
        assertEquals(Gender.MALE, genderCaptor.getValue());
        assertEquals(LocalDate.of(2005, 1, 15), dateCaptor.getAllValues().get(0));
        assertEquals(LocalDate.of(2023, 9, 1), dateCaptor.getAllValues().get(1));
        assertEquals(mockSection, sectionCaptor.getValue());
        assertEquals(101, intCaptor.getValue()); // Verify rollNo
    }

    @Test
    void processStudentRow_throwsException_whenEmailExists() throws Exception {
        // --- 1. Arrange ---
        mockStudentHelperSuccess();

        // Mock the business rule violation
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // --- 2. Act & 3. Assert ---
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            // Call with the mock caches
            bulkImportService.processStudentRow(validStudentRow, mockRoleCache, mockSectionCache);
        });

        assertEquals("User with email 'john.doe@example.com' already exists.", e.getMessage());

        // Verify the helper was NEVER called
        verify(registerUserByRole, never()).RegisterStudent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void processStudentRow_throwsException_whenSectionNotFound() throws Exception {
        // --- 1. Arrange ---
        mockStudentHelperSuccess();

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(studentRepository.existsByEnrollmentNumber("S12345")).thenReturn(false);

        // --- NEW: Simulate a cache miss by passing an empty map ---
        Map<String, Section> emptySectionCache = Collections.emptyMap();

        // --- 2. Act & 3. Assert ---
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            // Call with the empty cache
            bulkImportService.processStudentRow(validStudentRow, mockRoleCache, emptySectionCache);
        });

        assertEquals("Section not found for class 'Class 10' and section 'A'.", e.getMessage());

        // Verify the helper was NEVER called
        verify(registerUserByRole, never()).RegisterStudent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * Mocks all CsvValidationHelper calls for the staff row.
     */
    private void mockStaffHelperSuccess() throws Exception {
        when(validationHelper.validateString(validStaffRow[0], "firstName")).thenReturn("Jane");
        when(validationHelper.validateString(validStaffRow[1], "lastName")).thenReturn("Smith");
        when(validationHelper.validateEmail(validStaffRow[3])).thenReturn("jane.smith@example.com");
        when(validationHelper.parseDate(validStaffRow[4], "dateOfBirth")).thenReturn(LocalDate.of(1990, 5, 20));
        when(validationHelper.parseEnum(Gender.class, validStaffRow[5], "gender")).thenReturn(Gender.FEMALE);
        when(validationHelper.validateString(validStaffRow[6], "employeeId")).thenReturn("T98765");
        when(validationHelper.parseDate(validStaffRow[7], "joiningDate")).thenReturn(LocalDate.of(2015, 8, 15));
        when(validationHelper.validateString(validStaffRow[8], "jobTitle")).thenReturn("Math Teacher");
        when(validationHelper.parseEnum(Department.class, validStaffRow[9], "department")).thenReturn(Department.ACADEMICS);
        when(validationHelper.parseEnum(StaffType.class, validStaffRow[10], "staffType")).thenReturn(StaffType.TEACHER);
    }

    @Test
    void processStaffRow_success() throws Exception {
        // --- 1. Arrange ---
        mockStaffHelperSuccess();

        // Mock business logic checks
        when(userRepository.existsByEmail("jane.smith@example.com")).thenReturn(false);
        when(staffRepository.existsByEmployeeId("T98765")).thenReturn(false);

        // --- 2. Act ---
        bulkImportService.processStaffRow(validStaffRow, mockRoleCache);

        // --- 3. Assert ---

        // Verify the helper was called once with the correct, parsed data
        verify(registerUserByRole, times(1)).RegisterStaff(
                stringCaptor.capture(), // email
                stringCaptor.capture(), // employeeId
                stringCaptor.capture(), // DEFAULT_PASSWORD
                roleCaptor.capture(),   // staffRole
                stringCaptor.capture(), // firstName
                stringCaptor.capture(), // lastName
                stringCaptor.capture(), // middleName
                dateCaptor.capture(),   // dob
                genderCaptor.capture(), // gender
                dateCaptor.capture(),   // joiningDate
                stringCaptor.capture(), // jobTitle
                departmentCaptor.capture(), // department
                staffTypeCaptor.capture(), // staffType
                rowCaptor.capture()      // full row
        );

        // Verify the captured values
        assertEquals("jane.smith@example.com", stringCaptor.getAllValues().get(0));
        assertEquals("T98765", stringCaptor.getAllValues().get(1));
        assertEquals("Jane", stringCaptor.getAllValues().get(3));
        assertEquals("Smith", stringCaptor.getAllValues().get(4));
        assertEquals(mockStaffRole, roleCaptor.getValue());
        assertEquals(Department.ACADEMICS, departmentCaptor.getValue());
        assertEquals(StaffType.TEACHER, staffTypeCaptor.getValue());
        assertArrayEquals(validStaffRow, rowCaptor.getValue()); // Verify the *full row* was passed
    }
}