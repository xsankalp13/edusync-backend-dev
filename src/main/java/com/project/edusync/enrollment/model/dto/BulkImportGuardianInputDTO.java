package com.project.edusync.enrollment.model.dto;

import lombok.Data;

/**
 * Parsed guardian row payload used by student-guardian bulk import flow.
 */
@Data
public class BulkImportGuardianInputDTO {
    private String studentEnrollmentNumber;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private String phoneNumber;
    private String relationshipType;
    private String occupation;
    private String employer;
    private boolean primaryContact;
    private boolean canPickup;
    private boolean financialContact;
    private boolean canViewGrades;
}

