package com.project.edusync.hrms.dto.bank;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating or updating a staff member's bank details.
 * accountNumber is stored AES-encrypted via the existing EncryptedStringConverter.
 */
public record BankDetailsUpdateDTO(
        @NotBlank(message = "Account holder name is required")
        String accountHolderName,

        @NotBlank(message = "Account number is required")
        String accountNumber,

        @NotBlank(message = "IFSC code is required")
        @Pattern(regexp = "[A-Z]{4}0[A-Z0-9]{6}", message = "IFSC code format is invalid (e.g. HDFC0001234)")
        String ifscCode,

        /** Bank name resolved client-side from IFSC (optional but captured for convenience) */
        String bankName,

        /** SAVINGS or CURRENT */
        String accountType
) {}
