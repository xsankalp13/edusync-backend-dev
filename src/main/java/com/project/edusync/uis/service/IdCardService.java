package com.project.edusync.uis.service;

import java.util.UUID;

/**
 * Service for generating ID card PDFs for students and staff.
 * Supports single card generation, batch generation (A4 sheets),
 * and self-service generation for the authenticated user.
 * <p>
 * All external-facing methods accept UUIDs (not internal numeric IDs).
 * Template parameter selects the visual design: "classic", "modern", or "minimal".
 * </p>
 */
public interface IdCardService {

    /**
     * Generates a single student ID card PDF (landscape CR80 format).
     *
     * @param studentId The database ID of the student.
     * @param template  The template style ("classic", "modern", "minimal").
     * @return PDF bytes for the student's ID card (front + back).
     */
    byte[] generateStudentIdCard(Long studentId, String template);

    /**
     * Generates a single staff ID card PDF (portrait CR80 format).
     *
     * @param staffId  The database ID of the staff member.
     * @param template The template style ("classic", "modern", "minimal").
     * @return PDF bytes for the staff member's ID card (front + back).
     */
    byte[] generateStaffIdCard(Long staffId, String template);

    /**
     * Generates a batch PDF containing ID cards for all active students
     * in a given section (identified by UUID), rendered on A4 pages (4 cards per page).
     *
     * @param sectionUuid The UUID of the section whose students to generate cards for.
     * @param template    The template style ("classic", "modern", "minimal").
     * @return PDF bytes containing all student ID cards on A4 sheets.
     */
    byte[] generateBatchStudentIdCards(UUID sectionUuid, String template);

    /**
     * Generates a batch PDF containing ID cards for all staff members,
     * rendered on A4 pages (4 cards per page).
     *
     * @param template The template style ("classic", "modern", "minimal").
     * @return PDF bytes containing all staff ID cards on A4 sheets.
     */
    byte[] generateBatchStaffIdCards(String template);

    /**
     * Generates the ID card PDF for the currently authenticated user.
     * Internally resolves whether the user is a Student or Staff
     * and renders the appropriate card template.
     *
     * @param userId   The IAM user ID from the JWT token.
     * @param template The template style ("classic", "modern", "minimal").
     * @return PDF bytes for the user's ID card.
     * @throws com.project.edusync.common.exception.ResourceNotFoundException
     *         if the user has no Student or Staff profile.
     */
    byte[] generateMyIdCard(Long userId, String template);

    /**
     * Generates self-service ID card HTML (rendered Thymeleaf output) for preview/embedding use-cases.
     *
     * @param userId   The IAM user ID from the JWT token.
     * @param template The template style ("classic", "modern", "minimal").
     * @return Fully rendered HTML string.
     */
    String generateMyIdCardHtml(Long userId, String template);
}
