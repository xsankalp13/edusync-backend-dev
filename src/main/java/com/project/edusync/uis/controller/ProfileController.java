package com.project.edusync.uis.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.uis.model.dto.profile.ComprehensiveUserProfileResponseDTO;
import com.project.edusync.uis.model.dto.profile.AddressDTO;
import com.project.edusync.uis.model.dto.profile.GuardianProfileDTO;
import com.project.edusync.uis.model.dto.profile.ProfileImageUploadCompleteRequestDTO;
import com.project.edusync.uis.model.dto.profile.ProfileImageUploadInitRequestDTO;
import com.project.edusync.uis.model.dto.profile.ProfileImageUploadInitResponseDTO;
import com.project.edusync.uis.model.dto.profile.StudentMedicalAllergyDTO;
import com.project.edusync.uis.model.dto.profile.StudentMedicalRecordDTO;
import com.project.edusync.uis.model.dto.profile.UserProfileDTO;
import com.project.edusync.uis.model.dto.profile.UserProfileUpdateDTO;
import com.project.edusync.uis.service.IdCardService;
import com.project.edusync.uis.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing User Profiles.
 * <p>
 * This controller serves two main audiences:
 * 1. The authenticated user managing their own profile (/me endpoints).
 * 2. Administrators managing other users' profiles (/{userId} endpoints).
 * </p>
 */
@RestController
@RequestMapping("${api.url}/profile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile Management", description = "Endpoints for viewing and updating user profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final IdCardService idCardService;
    private final AuthUtil authUtil;

    // =================================================================================
    // SELF-SERVICE ENDPOINTS (/me)
    // =================================================================================

    /**
     * Get the currently logged-in user's comprehensive profile.
     * <p>
     * This returns the "Full Picture" — not just the name/email, but also
     * their specific role details (e.g., if they are a Student, it returns
     * student details; if a Teacher, teacher details).
     * </p>
     *
     * @return The full profile hierarchy for the current user.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('profile:read:own')")
    @Operation(summary = "Get My Profile", description = "Retrieves the full profile details (Personal + Role Specific) of the currently logged-in user.")
    public ResponseEntity<ComprehensiveUserProfileResponseDTO> getMyProfile() {
        // We use AuthUtil to safely extract the ID from the SecurityContext (JWT)
        Long currentUserId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(profileService.getProfileByUserId(currentUserId));
    }

    /**
     * Update the currently logged-in user's basic profile information.
     * <p>
     * Allows users to update fields like Bio, Preferred Name, etc.
     * It does NOT allow updating sensitive system fields (like Role or Staff ID).
     * </p>
     *
     * @param updateDto The subset of fields the user is allowed to change.
     * @return The updated profile state.
     */
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Update My Profile", description = "Updates editable personal details (Bio, Preferred Name, etc.) for the current user.")
    public ResponseEntity<UserProfileDTO> updateMyProfile(@Valid @RequestBody UserProfileUpdateDTO updateDto) {
        Long currentUserId = authUtil.getCurrentUserId();
        UserProfileDTO updatedProfile = profileService.updateProfileByUserId(currentUserId, updateDto);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/me/image/upload-init")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Initialize Profile Image Upload", description = "Creates secure provider-specific upload instructions for the current user's profile image.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload instructions created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid upload request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProfileImageUploadInitResponseDTO> initMyProfileImageUpload(
            @Valid @RequestBody ProfileImageUploadInitRequestDTO request) {

        Long currentUserId = authUtil.getCurrentUserId();
        log.info("Profile image upload-init requested for userId={} fileName={} contentType={} sizeBytes={}",
                currentUserId, request.getFileName(), request.getContentType(), request.getSizeBytes());

        ProfileImageUploadInitResponseDTO response = profileService.initiateProfileImageUpload(currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/image/upload-complete")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Complete Profile Image Upload", description = "Finalizes profile image upload and stores the secure URL for the current user's profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile image saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid completion payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<UserProfileDTO> completeMyProfileImageUpload(
            @Valid @RequestBody ProfileImageUploadCompleteRequestDTO request) {

        Long currentUserId = authUtil.getCurrentUserId();
        log.info("Profile image upload-complete requested for userId={} objectKey={}",
                currentUserId, request.getObjectKey());

        UserProfileDTO updatedProfile = profileService.completeProfileImageUpload(currentUserId, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/me/addresses")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Add My Address", description = "Adds a new address for the authenticated user profile.")
    public ResponseEntity<AddressDTO> addMyAddress(@Valid @RequestBody AddressDTO request) {
        Long currentUserId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(profileService.addMyAddress(currentUserId, request));
    }

    @PutMapping("/me/addresses/{id}")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Update My Address", description = "Updates an existing address owned by the authenticated user.")
    public ResponseEntity<AddressDTO> updateMyAddress(@PathVariable Long id, @Valid @RequestBody AddressDTO request) {
        Long currentUserId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(profileService.updateMyAddress(currentUserId, id, request));
    }

    @DeleteMapping("/me/addresses/{id}")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Delete My Address", description = "Deletes an existing address owned by the authenticated user.")
    public ResponseEntity<Void> deleteMyAddress(@PathVariable Long id) {
        Long currentUserId = authUtil.getCurrentUserId();
        profileService.deleteMyAddress(currentUserId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/medical")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Update My Medical Record", description = "Updates medical contacts and insurance details for the authenticated student.")
    public ResponseEntity<StudentMedicalRecordDTO> updateMyMedicalRecord(@Valid @RequestBody StudentMedicalRecordDTO request) {
        Long currentUserId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(profileService.updateMyMedicalRecord(currentUserId, request));
    }

    @GetMapping("/me/medical")
    @PreAuthorize("hasAuthority('profile:read:own')")
    @Operation(summary = "Get My Medical Record", description = "Returns the authenticated student's medical record.")
    public ResponseEntity<StudentMedicalRecordDTO> getMyMedicalRecord() {
        Long currentUserId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(profileService.getMyMedicalRecord(currentUserId));
    }

    @PostMapping("/me/medical")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Create My Medical Record", description = "Creates the authenticated student's medical record once. Duplicate creation is blocked.")
    public ResponseEntity<StudentMedicalRecordDTO> createMyMedicalRecord(@Valid @RequestBody StudentMedicalRecordDTO request) {
        Long currentUserId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(profileService.createMyMedicalRecord(currentUserId, request));
    }

    @PostMapping("/me/medical/allergies")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Add My Allergy", description = "Adds an allergy to the authenticated student's medical record.")
    public ResponseEntity<StudentMedicalAllergyDTO> addMyMedicalAllergy(@Valid @RequestBody StudentMedicalAllergyDTO request) {
        Long currentUserId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(profileService.addMyMedicalAllergy(currentUserId, request));
    }

    @DeleteMapping("/me/medical/allergies/{id}")
    @PreAuthorize("hasAuthority('profile:update:own')")
    @Operation(summary = "Delete My Allergy", description = "Deletes an allergy from the authenticated student's medical record.")
    public ResponseEntity<Void> deleteMyMedicalAllergy(@PathVariable Long id) {
        Long currentUserId = authUtil.getCurrentUserId();
        profileService.deleteMyMedicalAllergy(currentUserId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/guardians")
    @PreAuthorize("hasAuthority('profile:read:own')")
    @Operation(summary = "Get My Guardians", description = "Returns guardians linked to the authenticated student. View-only access.")
    public ResponseEntity<List<GuardianProfileDTO>> getMyGuardians() {
        Long currentUserId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(profileService.getMyGuardians(currentUserId));
    }

    @GetMapping("/me/id-card")
    @PreAuthorize("hasAuthority('profile:read:own')")
    @Operation(summary = "Download My ID Card",
               description = "Generates and returns a PDF ID card for the currently authenticated user (student or staff).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ID card PDF generated successfully"),
            @ApiResponse(responseCode = "404", description = "No student or staff profile found for the current user")
    })
    public ResponseEntity<byte[]> downloadMyIdCard() {
        Long currentUserId = authUtil.getCurrentUserId();
        log.info("Self-service ID card download requested for userId={}", currentUserId);
        
        // Pass empty string so 'IdCardServiceImpl' falls back to 'school.id_card_template'
        byte[] pdf = idCardService.generateMyIdCard(currentUserId, "");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"my-id-card.pdf\"")
                .body(pdf);
    }

    @GetMapping("/me/id-card/preview-html")
    @PreAuthorize("hasAuthority('profile:read:own')")
    @Operation(summary = "Get My ID Card HTML Preview",
            description = "Renders and returns ID card HTML for the current user (for iframe/interactive preview use-cases).")
    public ResponseEntity<Map<String, String>> getMyIdCardPreviewHtml() {
        Long currentUserId = authUtil.getCurrentUserId();
        log.info("Self-service ID card HTML preview requested for userId={}", currentUserId);

        String html = idCardService.generateMyIdCardHtml(currentUserId, "");
        return ResponseEntity.ok(Map.of("html", html));
    }

    // ADMINISTRATIVE ENDPOINTS (/{userId})
    // =================================================================================

    /**
     * Get ANY user's profile by their User ID.
     * <p>
     * Restricted to Admins/Staff with 'profile:read:all' permission.
     * Used by School Admins to view details of any student, parent, or staff member.
     * </p>
     *
     * @param userId The target user's ID.
     * @return The target user's comprehensive profile.
     */
    @GetMapping("/{userId}")
//    @PreAuthorize("hasAuthority('profile:read:all')")
    @Operation(summary = "Get User Profile (Admin)", description = "Retrieves the full profile of any user. Requires administrative privileges.")
    public ResponseEntity<ComprehensiveUserProfileResponseDTO> getProfileByUserId(@PathVariable Long userId) {
        // Pass the requested userId directly to the service
        return ResponseEntity.ok(profileService.getProfileByUserId(userId));
    }

    /**
     * Update ANY user's profile by their User ID.
     * <p>
     * Restricted to Admins with 'profile:update:all' permission.
     * Useful for fixing typos in names, updating incorrect birth dates, etc.
     * </p>
     *
     * @param userId    The target user's ID.
     * @param updateDto The new data.
     * @return The updated profile.
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('profile:update:all')")
    @Operation(summary = "Update User Profile (Admin)", description = "Updates personal details for a specific user. Requires administrative privileges.")
    public ResponseEntity<UserProfileDTO> updateProfileByUserId(
            @PathVariable Long userId,
            @Valid @RequestBody UserProfileUpdateDTO updateDto) {

        UserProfileDTO updatedProfile = profileService.updateProfileByUserId(userId, updateDto);
        return ResponseEntity.ok(updatedProfile);
    }
}