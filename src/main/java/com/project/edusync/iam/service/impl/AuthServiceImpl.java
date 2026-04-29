package com.project.edusync.iam.service.impl;

import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.exception.iam.InvalidCredentialsException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.service.EmailService;
import com.project.edusync.iam.model.dto.auth.*;
import com.project.edusync.iam.model.dto.user.MeResponse;
import com.project.edusync.iam.model.dto.user.UserDetailsDto;
import com.project.edusync.iam.model.entity.RefreshToken;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.iam.service.AuthService;
import com.project.edusync.iam.service.PasswordResetTokenService;
import com.project.edusync.iam.service.RefreshTokenService;
import com.project.edusync.uis.model.dto.AddressDTO;
import com.project.edusync.uis.model.entity.UserAddress;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;

    @Override
    @Transactional
    public LoginResponse loginUser(LoginRequest loginRequest, String ipAddress) {
        log.info("Attempting login for user: {}", loginRequest.username());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
            );
        } catch (BadCredentialsException | InternalAuthenticationServiceException e) {
            // Catches:
            // 1. BadCredentialsException (wrong password)
            // 2. InternalAuthenticationServiceException (our "user not found" exception)

            log.warn("Invalid login attempt for user: {}", loginRequest.username());
            // 2. RE-THROWING as custom exception 401
            throw new InvalidCredentialsException("Invalid username or password");
        }
        // 3. Taking out user from the authentication object
        User user = (User) authentication.getPrincipal();

        // 4. Generate tokens, now passing IP

        log.info("Generating access token for user");
        String accessToken = authUtil.generateAccessToken(
                user.getUsername(),
                user.getRoles(),
                user.getId(),
                loginRequest.academicYearId()
        );
        String refreshToken = null;
        if(loginRequest.rememberMe()){
            log.info("Generating refresh token");
            refreshToken = refreshTokenService.createRefreshToken(user, ipAddress).getToken();
        }
        log.info("Fetching roles of the user");
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
        String profileUrl = profile != null ? profile.getProfileUrl() : null;
        String firstName = profile != null ? profile.getFirstName() : null;
        String lastName = profile != null ? profile.getLastName() : null;

        // 5. Prepare user DTO
        UserDetailsDto userDetailsDto = new UserDetailsDto(
                user.getId(),
                user.getUsername(),
                firstName,
                lastName,
                user.getEmail(),
                profileUrl,
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
        );
        boolean isFirstLogin = (user.getLastLoginTimestamp() == null);

        // 6. Update Last Login Timestamp (Conditional)
        if (!isFirstLogin) {
            // Only update if it's NOT the first login.
            // If it IS the first login, we leave it NULL so the flag persists
            // until they actually change the password.
            user.setLastLoginTimestamp(LocalDateTime.now());
            userRepository.save(user);
        } else {
            log.info("User {} is logging in for the first time. Password change required.", user.getUsername());
        }
        log.info("User logged in successfully");
        return new LoginResponse(accessToken, refreshToken, userDetailsDto, roles, isFirstLogin);
    }

    @Override
    @Transactional
    public void logoutUser(LogoutRequest logoutRequest) {
        log.info("Logging out user.");
        refreshTokenService.invalidateToken(logoutRequest.refreshToken());
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Refreshing access token.");

        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenRequest.refreshToken());
        User user = refreshToken.getUser();

        refreshTokenService.invalidateToken(refreshToken.getToken());
        String newRefreshToken = refreshTokenService.createRefreshToken(user, refreshToken.getIpAddress()).getToken();
        String newAccessToken = authUtil.generateAccessToken(
                user.getUsername(),
                user.getRoles(),
                user.getId(),
                refreshTokenRequest.academicYearId()
        );

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequest forgotPasswordRequest) {
        log.info("Password reset initiated for email: {}", forgotPasswordRequest.email());
        Optional<User> userOptional = userRepository.findByEmail(forgotPasswordRequest.email()); //
        if (userOptional.isEmpty()) {
            log.warn("Password reset attempted for non-existent email: {}", forgotPasswordRequest.email());
            return;
        }
        User user = userOptional.get();
        String token = passwordResetTokenService.createResetToken(user).getToken();
        emailService.sendPasswordResetEmail(user, token);
    }

    @Override
    @Transactional
    public void completePasswordReset(ResetPasswordRequest resetPasswordRequest) {
        log.info("Completing password reset.");

        passwordResetTokenService.validateResetToken(resetPasswordRequest.token());
        User user = passwordResetTokenService.getUserByToken(resetPasswordRequest.token());

        user.setPassword(passwordEncoder.encode(resetPasswordRequest.newPassword()));
        userRepository.save(user);

        passwordResetTokenService.invalidateToken(resetPasswordRequest.token());
    }

    // --- FIX: Corrected method signature to match interface and controller ---
    @Override
    @Transactional(readOnly = true)
    public MeResponse getUserProfile(UserDetails userDetails) {
        log.info("Fetching profile for user: {}", userDetails.getUsername());

        // Find user by username from the token
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found : " + userDetails.getUsername()));

        // Find the associated user profile
        UserProfile userProfile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found for user: " + user.getUsername()));

        // Map both entities to the final DTO
        return mapToMeResponse(user, userProfile);
    }

    @Override
    public MeResponse getUserProfile(UserDetailsDto userDetailsDto) {
        return null;
    }

    /**
     * Private helper to map a User and UserProfile entity to the MeResponse DTO.
     * This encapsulates the complex mapping logic.
     *
     * @param user        The authenticated User entity.
     * @param userProfile The associated UserProfile entity.
     * @return A fully populated MeResponse DTO.
     */
    private MeResponse mapToMeResponse(User user, UserProfile userProfile) {

        // 1. Map roles from Set<Role> to Set<String>
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName) //
                .collect(Collectors.toSet());

        // 2. Map addresses from Set<UserAddress> to Set<AddressDto>
        Set<AddressDTO> addresses = userProfile.getAddresses().stream() //
                .map(this::mapToAddressDto) // Calling the other helper
                .collect(Collectors.toSet());

        // 3. Construct the final DTO
        return new MeResponse(
                user.getId(), //
                user.getUsername(),
                user.getEmail(),
                roles,
                userProfile.getId(),
                userProfile.getFirstName(),
                userProfile.getLastName(),
                userProfile.getPreferredName(),
                userProfile.getProfileUrl(),
                userProfile.getDateOfBirth(),
                userProfile.getGender(), //
                addresses
        );
    }

    /**
     * Private helper to map a UserAddress entity to an AddressDto.
     * This flattens the UserAddress -> Address relationship.
     *
     * @param userAddress The UserAddress join entity.
     * @return A flattened AddressDto.
     */
    private AddressDTO mapToAddressDto(UserAddress userAddress) {
        return new AddressDTO(
                userAddress.getAddress().getId(),
                userAddress.getAddressType(),
                userAddress.getAddress().getAddressLine1(),
                userAddress.getAddress().getAddressLine2(),
                userAddress.getAddress().getCity(),
                userAddress.getAddress().getStateProvince(),
                userAddress.getAddress().getPostalCode(),
                userAddress.getAddress().getCountry()
        );
    }
}