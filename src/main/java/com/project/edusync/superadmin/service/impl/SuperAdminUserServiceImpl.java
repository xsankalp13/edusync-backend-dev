package com.project.edusync.superadmin.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.model.dto.response.MessageResponse;
import com.project.edusync.common.settings.service.AppSettingService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.RefreshTokenRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.superadmin.model.dto.GuardianLinkedStudentDto;
import com.project.edusync.superadmin.model.dto.GuardianSummaryDto;
import com.project.edusync.superadmin.model.dto.SuperAdminResetPasswordResponseDto;
import com.project.edusync.superadmin.service.SuperAdminUserService;
import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.StudentGuardianRelationship;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.GuardianRepository;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentGuardianRelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuperAdminUserServiceImpl implements SuperAdminUserService {

    private final GuardianRepository guardianRepository;
    private final StaffRepository staffRepository;
    private final StudentGuardianRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppSettingService appSettingService;

    private static final Map<String, String> GUARDIAN_SORT_FIELDS = Map.of(
            "createdAt", "createdAt",
            "name", "userProfile.firstName",
            "email", "userProfile.user.email",
            "phoneNumber", "phoneNumber",
            "username", "userProfile.user.username"
    );

    @Override
    @Transactional(readOnly = true)
    public Page<GuardianSummaryDto> listGuardians(String search, Pageable pageable) {
        Pageable resolved = resolveGuardianSort(pageable);
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;

        Page<Guardian> guardianPage = guardianRepository.findForSuperAdmin(normalizedSearch, resolved);
        if (guardianPage.isEmpty()) {
            return guardianPage.map(this::toEmptyGuardianSummary);
        }

        List<Long> guardianIds = guardianPage.getContent().stream().map(Guardian::getId).toList();
        List<StudentGuardianRelationship> relationships = relationshipRepository.findAllWithStudentGraphByGuardianIds(guardianIds);
        Map<Long, List<StudentGuardianRelationship>> byGuardianId = new HashMap<>();
        for (StudentGuardianRelationship relationship : relationships) {
            byGuardianId.computeIfAbsent(relationship.getGuardian().getId(), ignored -> new ArrayList<>()).add(relationship);
        }

        return guardianPage.map(guardian -> toGuardianSummary(guardian, byGuardianId.getOrDefault(guardian.getId(), List.of())));
    }

    @Override
    @Transactional
    public MessageResponse forceLogout(UUID staffUuid) {
        User user = resolveStaffUser(staffUuid);

        refreshTokenRepository.invalidateAllActiveByUser(user, Instant.now());
        return new MessageResponse("User sessions invalidated successfully.");
    }

    @Override
    @Transactional
    public MessageResponse invalidateAllSessions() {
        int invalidated = refreshTokenRepository.invalidateAllActive(Instant.now());
        return new MessageResponse("All active sessions have been invalidated. " + invalidated + " sessions terminated.");
    }

    @Override
    @Transactional
    public SuperAdminResetPasswordResponseDto resetPassword(UUID staffUuid, String newPassword) {
        User user = resolveStaffUser(staffUuid);

        String effectivePassword = newPassword;
        String temporaryPassword = null;

        if (!StringUtils.hasText(effectivePassword)) {
            effectivePassword = appSettingService.getValue("auth.default_password", "");
            if (!StringUtils.hasText(effectivePassword)) {
                effectivePassword = generateTemporaryPassword();
                temporaryPassword = effectivePassword;
            }
        }

        validatePasswordPolicy(effectivePassword);

        user.setPassword(passwordEncoder.encode(effectivePassword));
        // Existing login flow treats null lastLoginTimestamp as must-change-password state.
        user.setLastLoginTimestamp(null);
        userRepository.save(user);

        return new SuperAdminResetPasswordResponseDto("Password reset successfully.", temporaryPassword);
    }

    private User resolveStaffUser(UUID staffUuid) {
        Staff staff = staffRepository.findByUuid(staffUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffUuid));

        User user = staff.getUser();
        if (user == null && staff.getUserProfile() != null) {
            user = staff.getUserProfile().getUser();
        }
        if (user == null) {
            throw new ResourceNotFoundException("User", "staffUuid", staffUuid);
        }

        return user;
    }

    private GuardianSummaryDto toGuardianSummary(Guardian guardian, List<StudentGuardianRelationship> relationships) {
        UserProfile profile = guardian.getUserProfile();

        String relation = relationships.stream()
                .filter(StudentGuardianRelationship::isPrimaryContact)
                .map(StudentGuardianRelationship::getRelationshipType)
                .findFirst()
                .orElseGet(() -> relationships.stream()
                        .map(StudentGuardianRelationship::getRelationshipType)
                        .findFirst()
                        .orElse(null));

        boolean primaryContact = relationships.stream().anyMatch(StudentGuardianRelationship::isPrimaryContact);

        List<GuardianLinkedStudentDto> linkedStudents = relationships.stream()
                .map(StudentGuardianRelationship::getStudent)
                .sorted(Comparator.comparing(Student::getEnrollmentNumber, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toLinkedStudentDto)
                .toList();

        boolean userActive = profile.getUser() != null && profile.getUser().isActive();

        return new GuardianSummaryDto(
                guardian.getUuid() == null ? null : guardian.getUuid().toString(),
                fullName(profile.getFirstName(), profile.getMiddleName(), profile.getLastName()),
                profile.getUser() == null ? null : profile.getUser().getUsername(),
                profile.getUser() == null ? null : profile.getUser().getEmail(),
                guardian.getPhoneNumber(),
                relation,
                guardian.getOccupation(),
                guardian.getEmployer(),
                primaryContact,
                guardian.isActive() && userActive,
                linkedStudents.size(),
                linkedStudents
        );
    }

    private GuardianSummaryDto toEmptyGuardianSummary(Guardian guardian) {
        return toGuardianSummary(guardian, List.of());
    }

    private GuardianLinkedStudentDto toLinkedStudentDto(Student student) {
        String studentName = fullName(
                student.getUserProfile().getFirstName(),
                student.getUserProfile().getMiddleName(),
                student.getUserProfile().getLastName()
        );

        return new GuardianLinkedStudentDto(
                student.getUuid() == null ? null : student.getUuid().toString(),
                studentName,
                student.getEnrollmentNumber(),
                student.getSection() == null || student.getSection().getAcademicClass() == null
                        ? null
                        : student.getSection().getAcademicClass().getName(),
                student.getSection() == null ? null : student.getSection().getSectionName()
        );
    }

    private Pageable resolveGuardianSort(Pageable pageable) {
        Sort resolvedSort;

        if (!pageable.getSort().isSorted()) {
            resolvedSort = Sort.by("createdAt").descending();
        } else {
            List<Sort.Order> mappedOrders = pageable.getSort().stream()
                    .map(order -> {
                        String mapped = GUARDIAN_SORT_FIELDS.getOrDefault(order.getProperty(), "createdAt");
                        return order.isAscending() ? Sort.Order.asc(mapped) : Sort.Order.desc(mapped);
                    })
                    .toList();
            resolvedSort = Sort.by(mappedOrders);
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolvedSort);
    }

    private String fullName(String firstName, String middleName, String lastName) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, firstName);
        appendPart(builder, middleName);
        appendPart(builder, lastName);
        return builder.toString().trim();
    }

    private void appendPart(StringBuilder builder, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }

    private void validatePasswordPolicy(String password) {
        int minLength = Integer.parseInt(appSettingService.getValue("auth.password.min_length", "8"));
        if (password == null || password.length() < minLength) {
            throw new EdusyncException("Password does not meet minimum length policy.", HttpStatus.BAD_REQUEST);
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!(hasUpper && hasLower && hasDigit && hasSpecial)) {
            throw new EdusyncException(
                    "Password must include uppercase, lowercase, number, and special character.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String generateTemporaryPassword() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            password.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return password.toString();
    }
}



