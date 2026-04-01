package com.project.edusync.superadmin.model.dto;

public record GuardianLinkedStudentDto(
        String studentUuid,
        String name,
        String enrollmentNumber,
        String className,
        String sectionName
) {
}

