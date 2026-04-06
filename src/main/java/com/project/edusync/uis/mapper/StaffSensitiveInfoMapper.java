package com.project.edusync.uis.mapper;

import com.project.edusync.uis.model.dto.profile.StaffSensitiveInfoDTO;
import com.project.edusync.uis.model.entity.StaffSensitiveInfo;
import org.springframework.stereotype.Component;

@Component
public class StaffSensitiveInfoMapper {

    public StaffSensitiveInfoDTO toMaskedDto(StaffSensitiveInfo info) {
        if (info == null) {
            return null;
        }

        return new StaffSensitiveInfoDTO(
                maskAadhaar(info.getAadhaarNumber()),
                info.getPanNumber(),
                info.getPassportNumber(),
                info.getApaarId(),
                info.getBankName(),
                maskBankAccount(info.getBankAccountNumber()),
                info.getBankIfscCode(),
                info.getEmergencyContactName(),
                info.getEmergencyContactPhone(),
                info.getEmergencyContactRelation()
        );
    }

    private String maskAadhaar(String aadhaarNumber) {
        return maskLast4(aadhaarNumber, "XXXX-XXXX-");
    }

    private String maskBankAccount(String bankAccountNumber) {
        return maskLast4(bankAccountNumber, "XXXXXXXX");
    }

    private String maskLast4(String rawValue, String maskPrefix) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }

        String normalized = rawValue.replaceAll("\\s", "");
        if (normalized.length() <= 4) {
            return normalized;
        }

        return maskPrefix + normalized.substring(normalized.length() - 4);
    }
}


