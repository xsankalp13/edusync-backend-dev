package com.project.edusync.uis.model.enums;

/**
 * Defines the specific roles a Staff member can have.
 * This enum is used to enforce type safety and to determine
 * which details table (e.g., TeacherDetails) to join.
 */
public enum StaffType {
    TEACHER,
    LIBRARIAN,
    PRINCIPAL,
    SECURITY_GUARD,
    ADMINISTRATIVE_STAFF,
    OTHER
}