package com.project.edusync.hrms.dto.bank;

/**
 * Response DTO for a single staff member's bank account status.
 * The account number is always masked (last 4 digits only) for security.
 */
public record StaffBankStatusDTO(
        Long staffId,
        String uuid,
        String employeeId,
        String staffName,
        String designation,
        boolean hasBankDetails,
        boolean hasIfsc,
        String bankName,
        String ifscCode,
        String maskedAccountNumber,
        String accountType,
        String accountHolderName
) {}
