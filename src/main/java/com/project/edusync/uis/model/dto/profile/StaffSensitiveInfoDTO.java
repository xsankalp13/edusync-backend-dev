package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffSensitiveInfoDTO {
    private String aadhaarNumber;
    private String panNumber;
    private String passportNumber;
    private String apaarId;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
}

