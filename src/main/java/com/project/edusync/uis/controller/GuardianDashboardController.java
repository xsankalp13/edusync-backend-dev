package com.project.edusync.uis.controller;

import com.project.edusync.uis.model.dto.dashboard.IntelligenceResponseDTO;
import com.project.edusync.uis.model.dto.dashboard.OverviewResponseDTO;
import com.project.edusync.uis.service.GuardianDashboardService;
import com.project.edusync.uis.repository.GuardianRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.repository.StudentGuardianRelationshipRepository;
import com.project.edusync.uis.repository.UserProfileRepository;
import com.project.edusync.uis.repository.medical.StudentMedicalRecordRepository;
import com.project.edusync.uis.service.ProfileService;
import com.project.edusync.uis.model.dto.profile.ComprehensiveUserProfileResponseDTO;
import com.project.edusync.uis.model.dto.profile.StudentMedicalRecordDTO;
import com.project.edusync.uis.model.dto.profile.StudentMedicalAllergyDTO;
import com.project.edusync.uis.model.dto.profile.GuardianChildHealthUpdateRequestDTO;
import com.project.edusync.uis.model.entity.medical.StudentMedicalRecord;
import com.project.edusync.uis.model.entity.medical.StudentMedicalAllergy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import com.project.edusync.common.security.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("${api.url}/guardian/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guardian Dashboard", description = "Aggregated dashboard for all students linked to the guardian")
public class GuardianDashboardController {
    private final GuardianDashboardService guardianDashboardService;
    private final AuthUtil authUtil;
    private final GuardianRepository guardianRepository;
    private final StudentRepository studentRepository;
    private final StudentGuardianRelationshipRepository relationshipRepository;
    private final ProfileService profileService;
    private final UserProfileRepository userProfileRepository;
    private final StudentMedicalRecordRepository studentMedicalRecordRepository;

    @GetMapping("/intelligence")
    @PreAuthorize("hasRole('GUARDIAN')")
    @Operation(summary = "Get dashboard intelligence for all linked students", description = "Returns dashboard intelligence for each student linked to the logged-in guardian.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard intelligence fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<IntelligenceResponseDTO>> getAllLinkedStudentsDashboardIntelligence() {
        Long userId = authUtil.getCurrentUserId();
        Long academicYearId = authUtil.getCurrentAcademicYearId();
        List<IntelligenceResponseDTO> dashboards = guardianDashboardService.getAllLinkedStudentsDashboardIntelligence(userId, academicYearId);
        return ResponseEntity.ok(dashboards);
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('GUARDIAN')")
    @Operation(summary = "Get dashboard overview for all linked students", description = "Returns dashboard overview for each student linked to the logged-in guardian.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard overview fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<OverviewResponseDTO>> getAllLinkedStudentsDashboardOverview() {
        Long userId = authUtil.getCurrentUserId();
        Long academicYearId = authUtil.getCurrentAcademicYearId();
        List<OverviewResponseDTO> dashboards = guardianDashboardService.getAllLinkedStudentsDashboardOverview(userId, academicYearId);
        return ResponseEntity.ok(dashboards);
    }

    @GetMapping("/profile/{childId}")
    @PreAuthorize("hasAnyRole('GUARDIAN','PARENT')")
    @Operation(summary = "Get full profile for a linked child (by studentId)", description = "Returns the comprehensive profile for a child identified by their student id. Guardian must be linked to the student.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ComprehensiveUserProfileResponseDTO> getLinkedChildProfile(@PathVariable Long childId) {
        Long userId = authUtil.getCurrentUserId();

        // Resolve guardian for current user
        var guardianOpt = guardianRepository.findByUserProfile_User_Id(userId);
        if (guardianOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        var guardian = guardianOpt.get();

        // Resolve student by provided studentId (childId)
        var studentOpt = studentRepository.findById(childId);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        var student = studentOpt.get();

        // Verify mapping between guardian and student
        boolean linked = relationshipRepository.existsByStudentAndGuardian(student, guardian);
        if (!linked) {
            return ResponseEntity.status(404).build();
        }

        // Build comprehensive profile using existing ProfileService (expects userId of the student's login account)
        Long studentUserId = student.getUserProfile().getUser().getId();
        ComprehensiveUserProfileResponseDTO profile = profileService.getProfileByUserId(studentUserId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/health/{childId}")
    @PreAuthorize("hasRole('GUARDIAN')")
    @Operation(summary = "Update child's health record (blood group + medical record)", description = "Allows a guardian to update the health information for a linked child.", security = @SecurityRequirement(name = "bearerAuth"))
    @Transactional
    public ResponseEntity<StudentMedicalRecordDTO> updateChildHealth(@PathVariable Long childId, @RequestBody GuardianChildHealthUpdateRequestDTO request) {
        Long userId = authUtil.getCurrentUserId();

        var guardianOpt = guardianRepository.findByUserProfile_User_Id(userId);
        if (guardianOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        var guardian = guardianOpt.get();

        var studentOpt = studentRepository.findById(childId);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        var student = studentOpt.get();

        boolean linked = relationshipRepository.existsByStudentAndGuardian(student, guardian);
        if (!linked) {
            return ResponseEntity.status(404).build();
        }

        // 1) Update blood group on UserProfile
        if (request.getBloodGroup() != null) {
            var profile = student.getUserProfile();
            profile.setBloodGroup(request.getBloodGroup());
            userProfileRepository.save(profile);
        }

        // 2) Create or update StudentMedicalRecord
        StudentMedicalRecordDTO mrDto = request.getMedicalRecord();
        StudentMedicalRecord record;
        if (mrDto == null) {
            // Nothing to update for medical record
            return ResponseEntity.ok().build();
        }

        var existingOpt = studentMedicalRecordRepository.findByStudent_Id(student.getId());
        if (existingOpt.isPresent()) {
            record = existingOpt.get();
            record.setPrimaryCarePhysician(mrDto.getPhysicianName());
            record.setPhysicianPhone(mrDto.getPhysicianPhone());
            record.setInsuranceProvider(mrDto.getInsuranceProvider());
            record.setInsurancePolicyNumber(mrDto.getInsurancePolicyNumber());

            // Clear old allergies (orphanRemoval takes care of deletion)
            record.getAllergies().clear();
        } else {
            record = new StudentMedicalRecord();
            record.setStudent(student);
            record.setPrimaryCarePhysician(mrDto.getPhysicianName());
            record.setPhysicianPhone(mrDto.getPhysicianPhone());
            record.setInsuranceProvider(mrDto.getInsuranceProvider());
            record.setInsurancePolicyNumber(mrDto.getInsurancePolicyNumber());
        }

        // Add new allergies
        if (mrDto.getAllergies() != null) {
            for (var a : mrDto.getAllergies()) {
                StudentMedicalAllergy allergy = new StudentMedicalAllergy();
                allergy.setAllergyName(a.getAllergy());
                allergy.setReactionDetails(a.getNotes());
                // severity not provided in minimal payload; leave null or parse if present
                allergy.setMedicalRecord(record);
                record.getAllergies().add(allergy);
            }
        }

        // persist emergency contact fields (if provided)
        record.setEmergencyContactName(mrDto.getEmergencyContactName());
        record.setEmergencyContactPhone(mrDto.getEmergencyContactPhone());

        StudentMedicalRecord saved = studentMedicalRecordRepository.save(record);

        // Build response DTO from saved entity (canonical persisted state)
        var allergiesDto = saved.getAllergies().stream()
                .map(al -> new StudentMedicalAllergyDTO(al.getId(), al.getAllergyName(), al.getSeverity() == null ? null : al.getSeverity().name(), al.getReactionDetails()))
                .toList();

        StudentMedicalRecordDTO responseDto = new StudentMedicalRecordDTO(
                saved.getId(),
                saved.getPrimaryCarePhysician(),
                saved.getPhysicianPhone(),
                saved.getInsuranceProvider(),
                saved.getInsurancePolicyNumber(),
                saved.getEmergencyContactName(),
                saved.getEmergencyContactPhone(),
                allergiesDto,
                List.of()
        );

        return ResponseEntity.ok(responseDto);
    }
}

