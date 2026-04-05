package com.project.edusync.iam.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.iam.model.dto.CreateTeacherRequestDTO;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.hrms.model.entity.StaffDesignation;
import com.project.edusync.hrms.repository.StaffDesignationRepository;
import com.project.edusync.uis.mapper.StaffMapper;
import com.project.edusync.uis.mapper.TeacherMapper;
import com.project.edusync.uis.mapper.UserMapper;
import com.project.edusync.uis.mapper.UserProfileMapper;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.entity.details.TeacherDetails;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.UserProfileRepository;
import com.project.edusync.uis.repository.details.TeacherDetailsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplFlowTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private StaffDesignationRepository staffDesignationRepository;
    @Mock private TeacherDetailsRepository teacherDetailsRepository;

    @Mock private UserMapper userMapper;
    @Mock private UserProfileMapper userProfileMapper;
    @Mock private StaffMapper staffMapper;
    @Mock private TeacherMapper teacherMapper;

    @InjectMocks
    private UserManagementServiceImpl service;

    @Test
    void createTeacherPersistsCategoryAndDesignationWhenMatched() {
        CreateTeacherRequestDTO request = buildTeacherRequest("tch-001", StaffCategory.TEACHING, 10L);
        StaffDesignation designation = buildDesignation(10L, StaffCategory.TEACHING);

        mockCommonTeacherCreation(request, designation);

        User created = service.createTeacher(request);

        ArgumentCaptor<Staff> staffCaptor = ArgumentCaptor.forClass(Staff.class);
        verify(staffRepository).save(staffCaptor.capture());

        Staff savedStaff = staffCaptor.getValue();
        assertEquals(StaffCategory.TEACHING, savedStaff.getCategory());
        assertEquals(designation, savedStaff.getDesignation());
        assertNotNull(created);
    }

    @Test
    void createTeacherRejectsWhenDesignationCategoryMismatchesStaffCategory() {
        CreateTeacherRequestDTO request = buildTeacherRequest("tch-002", StaffCategory.NON_TEACHING_SUPPORT, 11L);
        StaffDesignation designation = buildDesignation(11L, StaffCategory.TEACHING);

        mockCommonTeacherCreation(request, designation);

        EdusyncException ex = assertThrows(EdusyncException.class, () -> service.createTeacher(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(staffRepository, never()).save(any(Staff.class));
    }

    private void mockCommonTeacherCreation(CreateTeacherRequestDTO request, StaffDesignation designation) {
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        Role teacherRole = new Role();
        teacherRole.setName("ROLE_TEACHER");
        when(roleRepository.findByName("ROLE_TEACHER")).thenReturn(Optional.of(teacherRole));

        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded");

        User mappedUser = new User();
        mappedUser.setUsername(request.getUsername());
        mappedUser.setEmail(request.getEmail());
        when(userMapper.toEntity(request)).thenReturn(mappedUser);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(1001L);
            }
            return user;
        });

        UserProfile mappedProfile = new UserProfile();
        mappedProfile.setFirstName(request.getFirstName());
        mappedProfile.setLastName(request.getLastName());
        when(userProfileMapper.toEntity(request)).thenReturn(mappedProfile);

        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile linkedProfile = new UserProfile();
        linkedProfile.setId(2001L);
        linkedProfile.setFirstName(request.getFirstName());
        linkedProfile.setLastName(request.getLastName());
        linkedProfile.setUser(mappedUser);
        when(userProfileRepository.findByUser(any(User.class))).thenReturn(Optional.of(linkedProfile));

        Staff mappedStaff = new Staff();
        mappedStaff.setEmployeeId(request.getUsername());
        mappedStaff.setActive(true);
        when(staffMapper.toEntity(request)).thenReturn(mappedStaff);

        when(staffDesignationRepository.findById(request.getDesignationId())).thenReturn(Optional.of(designation));
        lenient().when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> {
            Staff staff = invocation.getArgument(0);
            if (staff.getId() == null) {
                staff.setId(3001L);
            }
            return staff;
        });

        lenient().when(teacherMapper.toEntity(request)).thenReturn(new TeacherDetails());
        lenient().when(teacherDetailsRepository.save(any(TeacherDetails.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreateTeacherRequestDTO buildTeacherRequest(String username, StaffCategory category, Long designationId) {
        CreateTeacherRequestDTO request = new CreateTeacherRequestDTO();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setFirstName("Test");
        request.setLastName("Teacher");
        request.setInitialPassword("Pass@123");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setJobTitle("Teacher");
        request.setHireDate(LocalDate.of(2025, 4, 1));
        request.setCategory(category);
        request.setDesignationId(designationId);
        request.setOfficeLocation("Main Campus");
        return request;
    }

    private StaffDesignation buildDesignation(Long id, StaffCategory category) {
        StaffDesignation designation = new StaffDesignation();
        designation.setId(id);
        designation.setActive(true);
        designation.setCategory(category);
        designation.setDesignationCode("PRT");
        designation.setDesignationName("Primary Teacher");
        return designation;
    }
}



