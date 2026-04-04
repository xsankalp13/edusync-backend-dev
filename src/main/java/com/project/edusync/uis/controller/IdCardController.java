package com.project.edusync.uis.controller;

import com.project.edusync.common.settings.model.dto.AppSettingRequestDto;
import com.project.edusync.common.settings.service.AppSettingService;
import com.project.edusync.uis.service.IdCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for ID card generation.
 * <p>
 * All endpoints are restricted to SCHOOL_ADMIN and SUPER_ADMIN roles.
 * Supports single card download and batch generation on A4 sheets.
 * Templates: "classic" (default), "modern", "minimal".
 * </p>
 */
@RestController
@RequestMapping("${api.url}/auth/admin/id-cards")
@PreAuthorize("hasAnyAuthority('ROLE_SCHOOL_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ID Card Generation", description = "Endpoints for generating student and staff ID card PDFs")
public class IdCardController {
    private final IdCardService idCardService;
    private final AppSettingService appSettingService;

    // ── Template Config Endpoint ─────────────────────────────────────────

    @PatchMapping("/template")
    @Operation(summary = "Set Master ID Card Template",
               description = "Allows School Admins to globally assign the ID card template for student and staff downloads.")
    public ResponseEntity<Void> setMasterTemplate(
            @RequestParam @Parameter(description = "Template style: classic, modern, or minimal") String template) {
        log.info("Admin setting master ID card template to: {}", template);
        
        AppSettingRequestDto config = new AppSettingRequestDto("school.id_card_template", template.toLowerCase());
        
        appSettingService.patchSettings(java.util.Collections.singletonList(config));
        return ResponseEntity.ok().build();
    }

    // ── Single Card Endpoints ────────────────────────────────────────────

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Generate Student ID Card",
               description = "Generates and returns a PDF ID card for a specific student (landscape CR80 format).")
    public ResponseEntity<byte[]> downloadStudentIdCard(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "classic") @Parameter(description = "Template style: classic, modern, or minimal") String template) {
        log.info("Admin request: generate ID card for studentId={}, template={}", studentId, template);
        byte[] pdf = idCardService.generateStudentIdCard(studentId, template);
        return buildPdfResponse(pdf, "student-id-" + studentId + ".pdf");
    }

    @GetMapping("/staff/{staffId}")
    @Operation(summary = "Generate Staff ID Card",
               description = "Generates and returns a PDF ID card for a specific staff member (portrait CR80 format).")
    public ResponseEntity<byte[]> downloadStaffIdCard(
            @PathVariable Long staffId,
            @RequestParam(defaultValue = "classic") @Parameter(description = "Template style: classic, modern, or minimal") String template) {
        log.info("Admin request: generate ID card for staffId={}, template={}", staffId, template);
        byte[] pdf = idCardService.generateStaffIdCard(staffId, template);
        return buildPdfResponse(pdf, "staff-id-" + staffId + ".pdf");
    }

    // ── Batch Endpoints ──────────────────────────────────────────────────

    @GetMapping("/students/batch")
    @Operation(summary = "Batch Generate Student ID Cards",
               description = "Generates ID cards for all active students in a section (by UUID), rendered on A4 pages (4 cards per page).")
    public ResponseEntity<byte[]> downloadBatchStudentIdCards(
            @RequestParam @Parameter(description = "UUID of the section") UUID sectionId,
            @RequestParam(defaultValue = "classic") @Parameter(description = "Template style: classic, modern, or minimal") String template) {
        log.info("Admin request: batch generate student ID cards for sectionId={}, template={}", sectionId, template);
        byte[] pdf = idCardService.generateBatchStudentIdCards(sectionId, template);
        return buildPdfResponse(pdf, "student-ids-section-" + sectionId + ".pdf");
    }

    @GetMapping("/staff/batch")
    @Operation(summary = "Batch Generate Staff ID Cards",
               description = "Generates ID cards for all active staff members, rendered on A4 pages (4 cards per page).")
    public ResponseEntity<byte[]> downloadBatchStaffIdCards(
            @RequestParam(defaultValue = "classic") @Parameter(description = "Template style: classic, modern, or minimal") String template) {
        log.info("Admin request: batch generate staff ID cards, template={}", template);
        byte[] pdf = idCardService.generateBatchStaffIdCards(template);
        return buildPdfResponse(pdf, "staff-ids-batch.pdf");
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
