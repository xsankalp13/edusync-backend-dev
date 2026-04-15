package com.project.edusync.admission.service.impl;

import com.project.edusync.admission.model.dto.ApplicantSignupRequest;
import com.project.edusync.admission.service.AdmissionSignupService;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.iam.UserAlreadyExistsException;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionSignupServiceImpl implements AdmissionSignupService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User signup(ApplicantSignupRequest request) {
        log.info("Processing applicant signup for: {}", request.getEmail());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new EdusyncException("Passwords do not match", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.findByUsername(request.getEmail()).isPresent() || userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        Role applicantRole = roleRepository.findByName("ROLE_APPLICANT")
                .orElseThrow(() -> new EdusyncException("System Error: ROLE_APPLICANT not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = new User();
        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setMobile(request.getMobile());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setRoles(Collections.singleton(applicantRole));

        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        // Assuming first name is the first part of full name for simplicity in signup
        String[] nameParts = request.getFullName().split(" ", 2);
        profile.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            profile.setLastName(nameParts[1]);
        }
        
        userProfileRepository.save(profile);

        log.info("Applicant signup successful. User ID: {}", savedUser.getId());
        return savedUser;
    }
}
