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
    FINANCE_ADMIN,
    AUDITOR,
    ADMINISTRATIVE_STAFF,
    /**
     * Manages all administrative functions for a specific school.
     * This is the highest-level user *within* a school.
     */
    SCHOOL_ADMIN,

    /**
     * Manages the entire EduSync SaaS platform.
     * This role is for EduSync.ai employees to manage schools,
     * platform configuration, and support.
     */
    SUPER_ADMIN,
    OTHER
}