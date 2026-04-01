package com.project.edusync.superadmin.model.dto;

import java.util.List;

public record GuardianSummaryDto(
        String guardianUuid,
        String name,
        String username,
        String email,
        String phoneNumber,
        String relation,
        String occupation,
        String employer,
        boolean primaryContact,
        boolean active,
        int linkedStudentCount,
        List<GuardianLinkedStudentDto> linkedStudents
) {
}

