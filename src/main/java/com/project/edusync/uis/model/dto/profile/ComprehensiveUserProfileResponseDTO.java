package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComprehensiveUserProfileResponseDTO {

    // 1. Basic Info (from UserProfile & User)
    private UserProfileResponseDTO basicProfile;

    // 2. Common Info (Addresses)
    private List<AddressDTO> addresses;

    // 3. Role-Specific Info (Conditionally populated)

    // This will be non-null if the user is a student
    private StudentProfileDTO studentDetails;

    // This will be non-null if the user is a staff member
    private StaffProfileDTO staffDetails;

    // This will be non-null if the user is a staff member with sensitive info captured
    private StaffSensitiveInfoDTO sensitiveInfo;

    // This will be non-null if the user is a guardian
    private GuardianProfileDTO guardianDetails;
}
