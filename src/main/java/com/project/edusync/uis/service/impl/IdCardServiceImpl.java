package com.project.edusync.uis.service.impl;

import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.settings.service.AppSettingService;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.uis.model.entity.*;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.service.IdCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link IdCardService}.
 * <p>
 * Orchestrates data fetching from repositories, image processing (Cloudinary → Base64),
 * QR code generation (vCard format), barcode generation (Code128),
 * and PDF rendering via the existing {@link PdfGenerationService}.
 * School branding is pulled dynamically from the WHITELABEL app settings.
 * </p>
 * <p>
 * Supports 3 template styles: "classic", "modern", "minimal".
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdCardServiceImpl implements IdCardService {

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final PdfGenerationService pdfGenerationService;
    private final AppSettingService appSettingService;
    private final TemplateEngine templateEngine;


    private static final Set<String> VALID_TEMPLATES = Set.of("classic", "modern", "minimal");
    private static final String ID_CARD_TEMPLATE_BASE = "id-card/";

    // ── Single Card Generation ───────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStudentIdCard(Long studentId, String template) {
        String tmpl = resolveTemplate(template);
        log.info("Generating ID card for studentId={}, template={}", studentId, tmpl);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        Map<String, Object> data = buildStudentCardData(student);
        return pdfGenerationService.generatePdfFromHtml(resolveStudentTemplateName(tmpl), data);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStaffIdCard(Long staffId, String template) {
        String tmpl = resolveTemplate(template);
        log.info("Generating ID card for staffId={}, template={}", staffId, tmpl);

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", staffId));

        Map<String, Object> data = buildStaffCardData(staff);
        return pdfGenerationService.generatePdfFromHtml(resolveStaffTemplateName(tmpl), data);
    }

    // ── Self-Service ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generateMyIdCard(Long userId, String template) {
        String tmpl = resolveTemplate(template);
        log.info("Generating self-service ID card for userId={}, template={}", userId, tmpl);

        // Try student first, then staff
        Optional<Student> studentOpt = studentRepository.findByUserProfile_User_Id(userId);
        if (studentOpt.isPresent()) {
            Map<String, Object> data = buildStudentCardData(studentOpt.get());
            return pdfGenerationService.generatePdfFromHtml(resolveStudentTemplateName(tmpl), data);
        }

        Optional<Staff> staffOpt = staffRepository.findByUserProfile_User_Id(userId);
        if (staffOpt.isPresent()) {
            Map<String, Object> data = buildStaffCardData(staffOpt.get());
            return pdfGenerationService.generatePdfFromHtml(resolveStaffTemplateName(tmpl), data);
        }

        throw new ResourceNotFoundException("Student or Staff", "userId", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateMyIdCardHtml(Long userId, String template) {
        String tmpl = resolveTemplate(template);
        log.info("Generating self-service ID card HTML preview for userId={}, template={}", userId, tmpl);

        Optional<Student> studentOpt = studentRepository.findByUserProfile_User_Id(userId);
        if (studentOpt.isPresent()) {
            Map<String, Object> data = buildStudentCardData(studentOpt.get());
            return renderTemplateToHtml(resolveStudentTemplateName(tmpl), data);
        }

        Optional<Staff> staffOpt = staffRepository.findByUserProfile_User_Id(userId);
        if (staffOpt.isPresent()) {
            Map<String, Object> data = buildStaffCardData(staffOpt.get());
            return renderTemplateToHtml(resolveStaffTemplateName(tmpl), data);
        }

        throw new ResourceNotFoundException("Student or Staff", "userId", userId);
    }

    // ── Batch Generation ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generateBatchStudentIdCards(UUID sectionUuid, String template) {
        String tmpl = resolveTemplate(template);
        log.info("Generating batch student ID cards for sectionUuid={}, template={}", sectionUuid, tmpl);

        List<Student> students = studentRepository.findAllBySectionUuidWithDetails(sectionUuid);
        if (students.isEmpty()) {
            throw new ResourceNotFoundException("Students", "sectionUuid", sectionUuid.toString());
        }

        // Generate each student's card individually (front + back = 2 pages per student)
        // then merge all into one PDF
        List<byte[]> individualPdfs = students.stream()
                .map(student -> {
                    Map<String, Object> data = buildStudentCardData(student);
                    return pdfGenerationService.generatePdfFromHtml(resolveStudentTemplateName(tmpl), data);
                })
                .collect(Collectors.toList());

        return mergePdfs(individualPdfs);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateBatchStaffIdCards(String template) {
        String tmpl = resolveTemplate(template);
        log.info("Generating batch staff ID cards for all active staff, template={}", tmpl);

        List<Staff> staffList = staffRepository.findAll();
        staffList = staffList.stream()
                .filter(s -> s.getUserProfile() != null
                        && s.getUserProfile().getUser() != null
                        && s.getUserProfile().getUser().isActive())
                .collect(Collectors.toList());

        if (staffList.isEmpty()) {
            throw new ResourceNotFoundException("Staff", "filter", "active");
        }

        List<byte[]> individualPdfs = staffList.stream()
                .map(staff -> {
                    Map<String, Object> data = buildStaffCardData(staff);
                    return pdfGenerationService.generatePdfFromHtml(resolveStaffTemplateName(tmpl), data);
                })
                .collect(Collectors.toList());

        return mergePdfs(individualPdfs);
    }

    /**
     * Merges multiple PDF byte arrays into a single PDF using Apache PDFBox.
     */
    private byte[] mergePdfs(List<byte[]> pdfs) {
        try {
            org.apache.pdfbox.multipdf.PDFMergerUtility merger = new org.apache.pdfbox.multipdf.PDFMergerUtility();
            ByteArrayOutputStream mergedOutput = new ByteArrayOutputStream();
            merger.setDestinationStream(mergedOutput);

            for (byte[] pdf : pdfs) {
                merger.addSource(new java.io.ByteArrayInputStream(pdf));
            }

            merger.mergeDocuments(org.apache.pdfbox.io.MemoryUsageSetting.setupMainMemoryOnly());
            return mergedOutput.toByteArray();
        } catch (Exception e) {
            log.error("Error merging PDFs: {}", e.getMessage(), e);
            throw new com.project.edusync.common.exception.finance.PdfGenerationException("Failed to merge batch PDFs", e);
        }
    }

    // ── Data Builders ────────────────────────────────────────────────────

    private Map<String, Object> buildStudentCardData(Student student) {
        UserProfile profile = student.getUserProfile();
        Map<String, Object> data = new HashMap<>();

        // School branding from AppSettings
        populateSchoolBranding(data);

        // Student info
        String fullName = buildFullName(profile.getFirstName(), profile.getMiddleName(), profile.getLastName());
        data.put("studentName", fullName);
        data.put("className", student.getSection().getAcademicClass().getName());
        data.put("sectionName", student.getSection().getSectionName());
        data.put("rollNo", student.getRollNo() != null ? String.valueOf(student.getRollNo()) : "N/A");
        data.put("enrollmentNumber", safe(student.getEnrollmentNumber()));
        data.put("dateOfBirth", profile.getDateOfBirth() != null
                ? profile.getDateOfBirth().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A");
        data.put("bloodGroup", safe(profile.getBloodGroup()));
        data.put("gender", safe(profile.getGender().toString()));
        data.put("academicYear", computeAcademicYear());
        data.put("studentCardTypeLabel", "Student ID Card");

        // Address — student's primary address
        populateStudentAddress(profile, data);

        // Emergency Contact — first primary-contact guardian
        populateEmergencyContact(student, data);

        // Photo
        data.put("photoBase64", pdfGenerationService.fetchRemoteImageAsBase64(profile.getProfileUrl()));

        // QR Code — rich vCard format
        String schoolName = (String) data.get("schoolName");
        try {
            String qrContent = buildStudentVCard(fullName, schoolName,
                    (String) data.get("className"), (String) data.get("sectionName"),
                    safe(student.getEnrollmentNumber()),
                    student.getRollNo() != null ? String.valueOf(student.getRollNo()) : "",
                    safe(profile.getBloodGroup()));
            data.put("qrCodeBase64", pdfGenerationService.generateQrCodeBase64(qrContent, 150));
        } catch (Exception e) {
            log.error("Failed to generate QR code for student {}: {}", student.getId(), e.getMessage());
            data.put("qrCodeBase64", "");
        }

        // Barcode — enrollment number
        try {
            String enrollNum = safe(student.getEnrollmentNumber());
            if (!enrollNum.isEmpty()) {
                data.put("barcodeBase64", pdfGenerationService.generateBarcodeBase64(enrollNum, 200, 40));
            } else {
                data.put("barcodeBase64", "");
            }
        } catch (Exception e) {
            log.error("Failed to generate barcode for student {}: {}", student.getId(), e.getMessage());
            data.put("barcodeBase64", "");
        }

        return data;
    }

    private Map<String, Object> buildStaffCardData(Staff staff) {
        UserProfile profile = staff.getUserProfile();
        Map<String, Object> data = new HashMap<>();

        // School branding
        populateSchoolBranding(data);

        // Staff info
        String fullName = buildFullName(profile.getFirstName(), profile.getMiddleName(), profile.getLastName());
        data.put("staffName", fullName);
        data.put("jobTitle", safe(staff.getJobTitle()));
        data.put("department", staff.getDepartment() != null ? staff.getDepartment().name() : "N/A");
        data.put("employeeId", safe(staff.getEmployeeId()));
        data.put("bloodGroup", safe(profile.getBloodGroup()));
        data.put("dateOfBirth", profile.getDateOfBirth() != null
                ? profile.getDateOfBirth().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A");
        data.put("hireDate", staff.getHireDate() != null
                ? staff.getHireDate().format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                : "N/A");
        data.put("academicYear", computeAcademicYear());
        data.put("staffCardTypeLabel", resolveStaffCardTypeLabel(safe(staff.getJobTitle())));

        // Address
        populateStudentAddress(profile, data);
        // Staff don't have guardians — use a generic emergency contact placeholder
        // (can be extended when staff emergency contact fields are added to the entity)
        data.put("emergencyContactName", "");
        data.put("emergencyContactPhone", "");
        data.put("emergencyContactRelation", "");

        // Photo
        data.put("photoBase64", pdfGenerationService.fetchRemoteImageAsBase64(profile.getProfileUrl()));

        // QR Code — rich vCard format
        String schoolName = (String) data.get("schoolName");
        try {
            String qrContent = buildStaffVCard(fullName, schoolName,
                    safe(staff.getJobTitle()),
                    staff.getDepartment() != null ? staff.getDepartment().name() : "",
                    safe(staff.getEmployeeId()));
            data.put("qrCodeBase64", pdfGenerationService.generateQrCodeBase64(qrContent, 150));
        } catch (Exception e) {
            log.error("Failed to generate QR code for staff {}: {}", staff.getId(), e.getMessage());
            data.put("qrCodeBase64", "");
        }

        // Barcode — employee ID
        try {
            String empId = safe(staff.getEmployeeId());
            if (!empId.isEmpty()) {
                data.put("barcodeBase64", pdfGenerationService.generateBarcodeBase64(empId, 200, 40));
            } else {
                data.put("barcodeBase64", "");
            }
        } catch (Exception e) {
            log.error("Failed to generate barcode for staff {}: {}", staff.getId(), e.getMessage());
            data.put("barcodeBase64", "");
        }

        return data;
    }


    // ── vCard Builders ───────────────────────────────────────────────────

    private String buildStudentVCard(String name, String school, String className,
                                     String section, String enrollment, String rollNo, String bloodGroup) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\n");
        sb.append("VERSION:3.0\n");
        sb.append("FN:").append(name).append("\n");
        if (!school.isEmpty()) {
            sb.append("ORG:").append(school).append("\n");
        }
        sb.append("TITLE:Student - Class ").append(className).append(" ").append(section).append("\n");
        StringBuilder note = new StringBuilder();
        if (!enrollment.isEmpty()) note.append("Enrollment: ").append(enrollment);
        if (!rollNo.isEmpty()) {
            if (!note.isEmpty()) note.append(" | ");
            note.append("Roll: ").append(rollNo);
        }
        if (!bloodGroup.isEmpty()) {
            if (!note.isEmpty()) note.append(" | ");
            note.append("Blood: ").append(bloodGroup);
        }
        if (!note.isEmpty()) {
            sb.append("NOTE:").append(note).append("\n");
        }
        sb.append("END:VCARD");
        return sb.toString();
    }

    private String buildStaffVCard(String name, String school, String jobTitle,
                                    String department, String employeeId) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\n");
        sb.append("VERSION:3.0\n");
        sb.append("FN:").append(name).append("\n");
        if (!school.isEmpty()) {
            sb.append("ORG:").append(school).append("\n");
        }
        if (!jobTitle.isEmpty()) {
            sb.append("TITLE:").append(jobTitle).append("\n");
        }
        StringBuilder note = new StringBuilder();
        if (!employeeId.isEmpty()) note.append("Employee ID: ").append(employeeId);
        if (!department.isEmpty()) {
            if (!note.isEmpty()) note.append(" | ");
            note.append("Dept: ").append(department);
        }
        if (!note.isEmpty()) {
            sb.append("NOTE:").append(note).append("\n");
        }
        sb.append("END:VCARD");
        return sb.toString();
    }
    // ── Address & Emergency Contact ─────────────────────────────────────

    private void populateStudentAddress(UserProfile profile, Map<String, Object> data) {
        try {
            if (profile.getAddresses() != null && !profile.getAddresses().isEmpty()) {
                // Find primary address first, then fallback to any
                UserAddress primaryAddr = profile.getAddresses().stream()
                        .filter(UserAddress::isPrimary)
                        .findFirst()
                        .orElse(profile.getAddresses().iterator().next());

                Address addr = primaryAddr.getAddress();
                StringBuilder sb = new StringBuilder();
                sb.append(addr.getAddressLine1());
                if (addr.getAddressLine2() != null && !addr.getAddressLine2().isBlank()) {
                    sb.append(", ").append(addr.getAddressLine2());
                }
                sb.append(", ").append(addr.getCity());
                sb.append(", ").append(addr.getStateProvince());
                sb.append(" - ").append(addr.getPostalCode());
                data.put("studentAddress", sb.toString());
            } else {
                data.put("studentAddress", "");
            }
        } catch (Exception e) {
            log.debug("Unable to load student address: {}", e.getMessage());
            data.put("studentAddress", "");
        }
    }

    private void populateEmergencyContact(Student student, Map<String, Object> data) {
        try {
            if (student.getGuardianRelationships() != null && !student.getGuardianRelationships().isEmpty()) {
                // Find primary contact guardian first, then fallback to any
                StudentGuardianRelationship primaryRel = student.getGuardianRelationships().stream()
                        .filter(StudentGuardianRelationship::isPrimaryContact)
                        .findFirst()
                        .orElse(student.getGuardianRelationships().iterator().next());

                Guardian guardian = primaryRel.getGuardian();
                UserProfile guardianProfile = guardian.getUserProfile();
                String guardianName = buildFullName(guardianProfile.getFirstName(),
                        guardianProfile.getMiddleName(), guardianProfile.getLastName());
                String guardianPhone = safe(guardian.getPhoneNumber()).trim();
                // Replace # prefix with +91 as requested, or ensure +91 prefix
                if (guardianPhone.startsWith("#")) {
                    guardianPhone = "+91 " + guardianPhone.substring(1).trim();
                } else if (!guardianPhone.isEmpty() && !guardianPhone.startsWith("+91")) {
                    guardianPhone = "+91 " + guardianPhone;
                }
                String relation = safe(primaryRel.getRelationshipType());

                data.put("emergencyContactName", guardianName);
                data.put("emergencyContactPhone", guardianPhone);
                data.put("emergencyContactRelation", relation);
            } else {
                data.put("emergencyContactName", "");
                data.put("emergencyContactPhone", "");
                data.put("emergencyContactRelation", "");
            }
        } catch (Exception e) {
            log.debug("Unable to load emergency contact: {}", e.getMessage());
            data.put("emergencyContactName", "");
            data.put("emergencyContactPhone", "");
            data.put("emergencyContactRelation", "");
        }
    }

    // ── School Branding ──────────────────────────────────────────────────

    private void populateSchoolBranding(Map<String, Object> data) {
        data.put("schoolName", appSettingService.getValue("school.name", "My School"));
        data.put("primaryColor", appSettingService.getValue("school.primary_color", "#1e3a5f"));
        data.put("accentColor", appSettingService.getValue("school.accent_color", "#c9a84c"));
        data.put("schoolAddress", appSettingService.getValue("school.address", ""));
        data.put("schoolPhone", appSettingService.getValue("school.phone", ""));
        data.put("schoolEmail", appSettingService.getValue("school.email", ""));
        data.put("schoolWebsite", appSettingService.getValue("school.website", ""));
        String shortName = appSettingService.getValue("school.short_name", "");
        data.put("schoolShortName", shortName.isBlank() ? (String)data.get("schoolName") : shortName);
        data.put("schoolTagline", appSettingService.getValue("school.tagline", ""));

        String headerMode = appSettingService.getValue("school.id_card_header_mode", "TEXT");
        String headerImageUrl = appSettingService.getValue("school.id_card_header_image_url", "");
        String headerImageBase64 = "";
        if ("IMAGE".equalsIgnoreCase(headerMode) && headerImageUrl != null && !headerImageUrl.isBlank()) {
            headerImageBase64 = pdfGenerationService.fetchRemoteImageAsBase64OrEmpty(headerImageUrl);
        }
        data.put("idCardHeaderMode", headerMode);
        data.put("idCardHeaderImageBase64", headerImageBase64);
        data.put("idCardHeaderImageEnabled", !headerImageBase64.isBlank());

        // Logo: prefer the runtime-configured URL, fall back to classpath logo
        String logoUrl = appSettingService.getValue("school.logo_url", "");
        if (logoUrl != null && !logoUrl.isBlank()) {
            data.put("schoolLogoBase64", pdfGenerationService.fetchRemoteImageAsBase64(logoUrl));
        } else {
            data.put("schoolLogoBase64", pdfGenerationService.loadSchoolLogoBase64());
        }

        // Signature: optional URL
        String signatureUrl = appSettingService.getValue("school.signature_url", "");
        if (signatureUrl != null && !signatureUrl.isBlank()) {
            data.put("signatureBase64", pdfGenerationService.fetchRemoteImageAsBase64(signatureUrl));
        } else {
            data.put("signatureBase64", "");
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────

    private String resolveTemplate(String template) {
        if (template != null && !template.isBlank() && VALID_TEMPLATES.contains(template.toLowerCase())) {
            return template.toLowerCase();
        }
        // Fall back to the globally designated school ID card template
        String defaultTemplate = appSettingService.getValue("school.id_card_template", "classic").toLowerCase();
        return VALID_TEMPLATES.contains(defaultTemplate) ? defaultTemplate : "classic";
    }

    private String renderTemplateToHtml(String templateName, Map<String, Object> data) {
        Context context = new Context();
        context.setVariables(data);
        return templateEngine.process(templateName, context);
    }

    private String buildFullName(String first, String middle, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isBlank()) sb.append(first.trim());
        if (middle != null && !middle.isBlank()) sb.append(" ").append(middle.trim());
        if (last != null && !last.isBlank()) sb.append(" ").append(last.trim());
        return sb.toString().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String resolveStudentTemplateName(String template) {
        return ID_CARD_TEMPLATE_BASE + "student-id-card-" + template;
    }

    private String resolveStaffTemplateName(String template) {
        return ID_CARD_TEMPLATE_BASE + "staff-id-card-" + template;
    }

    private String resolveStaffCardTypeLabel(String jobTitle) {
        String normalized = jobTitle == null ? "" : jobTitle.toLowerCase(Locale.ROOT);
        if (normalized.contains("principal")) {
            return "Principal ID Card";
        }
        if (normalized.contains("teacher")) {
            return "Teacher ID Card";
        }
        return "Staff ID Card";
    }

    /**
     * Computes the current academic year string (e.g., "2025-2026")
     * based on the configured academic year start month.
     */
    private String computeAcademicYear() {
        String startMonthStr = appSettingService.getValue("school.academic_year_start", "APRIL");
        Month startMonth;
        try {
            startMonth = Month.valueOf(startMonthStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            startMonth = Month.APRIL;
        }

        LocalDate now = LocalDate.now();
        int startYear = now.getMonthValue() >= startMonth.getValue() ? now.getYear() : now.getYear() - 1;
        return startYear + "-" + (startYear + 1);
    }
}
