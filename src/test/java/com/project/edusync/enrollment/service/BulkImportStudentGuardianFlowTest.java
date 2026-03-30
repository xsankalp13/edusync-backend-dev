package com.project.edusync.enrollment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.enrollment.model.dto.BulkImportGuardianInputDTO;
import com.project.edusync.enrollment.service.impl.BulkImportServiceImpl;
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
import com.project.edusync.uis.repository.GuardianRepository;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentGuardianRelationshipRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkImportStudentGuardianFlowTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private GuardianRepository guardianRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private StudentGuardianRelationshipRepository studentGuardianRelationshipRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Spy
    private CsvValidationHelper validationHelper;
    @Mock
    private RegisterUserByRole registerUserByRole;
    @Mock
    private SseEmitterRegistry sseEmitterRegistry;
    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private BulkImportServiceImpl bulkImportService;

    private Map<String, Role> roleCache;
    private Map<String, Section> sectionCache;

    @BeforeEach
    void setUp() {
        Role studentRole = new Role();
        studentRole.setName("ROLE_STUDENT");

        Role guardianRole = new Role();
        guardianRole.setName("ROLE_GUARDIAN");

        roleCache = Map.of(
                "ROLE_STUDENT", studentRole,
                "ROLE_GUARDIAN", guardianRole
        );

        Section section = new Section();
        AcademicClass academicClass = new AcademicClass();
        academicClass.setName("Class 10");
        section.setAcademicClass(academicClass);
        section.setSectionName("A");

        sectionCache = Map.of("Class 10:A", section);
    }

    @Test
    void processStudentRow_createsNewGuardians_whenNoExistingUserByPhoneOrEmail() {
        String[] studentRow = new String[]{
                "Aarav", "Kumar", "", "aarav@example.com", "2012-01-10",
                "7", "MALE", "ENR-1001", "2024-06-01", "Class 10", "A"
        };

        BulkImportGuardianInputDTO g1 = guardian("ENR-1001", "Neha", "Kumar", "neha@example.com", "+91 98765 43210", "Mother", true, true, true, true);
        BulkImportGuardianInputDTO g2 = guardian("ENR-1001", "Raj", "Kumar", "raj@example.com", "+91 99001 12233", "Father", false, true, true, true);

        when(userRepository.existsByUsername("ENR-1001")).thenReturn(false);
        when(userRepository.existsByEmail("aarav@example.com")).thenReturn(false);
        when(studentRepository.existsByEnrollmentNumber("ENR-1001")).thenReturn(false);

        Student student = new Student();
        when(registerUserByRole.RegisterStudent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(student);

        when(userRepository.findByUsername("+919876543210")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("neha@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("+919900112233")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("raj@example.com")).thenReturn(Optional.empty());

        Guardian createdG1 = new Guardian();
        Guardian createdG2 = new Guardian();
        when(registerUserByRole.RegisterGuardian(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(createdG1, createdG2);

        when(studentGuardianRelationshipRepository.findByStudentAndGuardian(any(), any()))
                .thenReturn(Optional.empty());
        when(studentGuardianRelationshipRepository.save(any(StudentGuardianRelationship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BulkImportServiceImpl.StudentRowProcessingResult result = bulkImportService.processStudentRow(
                studentRow,
                roleCache,
                sectionCache,
                List.of(g1, g2)
        );

        assertEquals("ENR-1001", result.getEnrollmentNumber());
        assertEquals(2, result.getGuardiansCreatedCount());
        assertEquals(2, result.getGuardiansLinkedCount());
        assertEquals(List.of("+919876543210", "+919900112233"), result.getGuardianUsernames());
    }

    @Test
    void processStudentRow_reusesExistingGuardian_whenUserAlreadyExists() {
        String[] studentRow = new String[]{
                "Diya", "Singh", "", "diya@example.com", "2011-08-20",
                "13", "FEMALE", "ENR-2001", "2024-06-01", "Class 10", "A"
        };

        BulkImportGuardianInputDTO g1 = guardian("ENR-2001", "Kavita", "Singh", "kavita@example.com", "98765-00000", "Aunt", false, true, true, false);

        when(userRepository.existsByUsername("ENR-2001")).thenReturn(false);
        when(userRepository.existsByEmail("diya@example.com")).thenReturn(false);
        when(studentRepository.existsByEnrollmentNumber("ENR-2001")).thenReturn(false);

        Student student = new Student();
        when(registerUserByRole.RegisterStudent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(student);

        User existingGuardianUser = new User();
        existingGuardianUser.setId(88L);
        Role guardianRole = new Role();
        guardianRole.setName("ROLE_GUARDIAN");
        existingGuardianUser.setRoles(Set.of(guardianRole));

        when(userRepository.findByUsername("9876500000")).thenReturn(Optional.of(existingGuardianUser));
        when(userRepository.findByEmail("kavita@example.com")).thenReturn(Optional.of(existingGuardianUser));

        UserProfile profile = new UserProfile();
        profile.setUser(existingGuardianUser);
        when(userProfileRepository.findByUser(existingGuardianUser)).thenReturn(Optional.of(profile));

        Guardian guardian = new Guardian();
        guardian.setUserProfile(profile);
        when(guardianRepository.findByUserProfile(profile)).thenReturn(Optional.of(guardian));
        when(guardianRepository.save(any(Guardian.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(studentGuardianRelationshipRepository.findByStudentAndGuardian(any(), any()))
                .thenReturn(Optional.empty());
        when(studentGuardianRelationshipRepository.save(any(StudentGuardianRelationship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BulkImportServiceImpl.StudentRowProcessingResult result = bulkImportService.processStudentRow(
                studentRow,
                roleCache,
                sectionCache,
                List.of(g1)
        );

        assertEquals(0, result.getGuardiansCreatedCount());
        assertEquals(1, result.getGuardiansLinkedCount());
        assertTrue(result.getGuardianUsernames().contains("9876500000"));

        verify(registerUserByRole, never()).RegisterGuardian(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private BulkImportGuardianInputDTO guardian(String enrollment,
                                                String firstName,
                                                String lastName,
                                                String email,
                                                String phone,
                                                String relation,
                                                boolean primary,
                                                boolean pickup,
                                                boolean financial,
                                                boolean grades) {
        BulkImportGuardianInputDTO dto = new BulkImportGuardianInputDTO();
        dto.setStudentEnrollmentNumber(enrollment);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email);
        dto.setPhoneNumber(phone);
        dto.setRelationshipType(relation);
        dto.setPrimaryContact(primary);
        dto.setCanPickup(pickup);
        dto.setFinancialContact(financial);
        dto.setCanViewGrades(grades);
        return dto;
    }
}

