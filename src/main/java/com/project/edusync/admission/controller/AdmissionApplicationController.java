package com.project.edusync.admission.controller;

import com.project.edusync.admission.model.dto.*;
import com.project.edusync.admission.service.AdmissionApplicationService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdmissionApplicationController {

    private final AdmissionApplicationService applicationService;
    private final UserRepository userRepository;

    /**
     * GET /api/v1/admission/applications/mine
     * Get my own application details (Applicant)
     */
    @GetMapping("${api.url}/admission/applications/mine")
    public ResponseEntity<ApplicationDetailDTO> getMyApplication(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(applicationService.getMyApplication(user));
    }

    // --- Section Save Endpoints ---

    @PutMapping("${api.url}/admission/applications/sections/1")
    public ResponseEntity<Void> saveSection1(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody StudentBasicDetailsDTO dto) {
        applicationService.saveStudentBasicDetails(getUser(userDetails), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("${api.url}/admission/applications/sections/2")
    public ResponseEntity<Void> saveSection2(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody AddressContactDTO dto) {
        applicationService.saveAddressContactDetails(getUser(userDetails), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("${api.url}/admission/applications/sections/3")
    public ResponseEntity<Void> saveSection3(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody ParentGuardianDTO dto) {
        applicationService.saveParentGuardianDetails(getUser(userDetails), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("${api.url}/admission/applications/sections/4")
    public ResponseEntity<Void> saveSection4(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody AcademicInfoDTO dto) {
        applicationService.saveAcademicInformation(getUser(userDetails), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("${api.url}/admission/applications/sections/5")
    public ResponseEntity<Void> saveSection5(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody DocumentUploadsDTO dto) {
        applicationService.saveDocumentUploads(getUser(userDetails), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("${api.url}/admission/applications/sections/6")
    public ResponseEntity<Void> saveSection6(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody AdmissionDetailsDTO dto) {
        applicationService.saveAdmissionDetails(getUser(userDetails), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("${api.url}/admission/applications/sections/7")
    public ResponseEntity<Void> saveSection7(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody MedicalInfoDTO dto) {
        applicationService.saveMedicalInformation(getUser(userDetails), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("${api.url}/admission/applications/sections/8")
    public ResponseEntity<Void> saveSection8(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody TransportDetailsDTO dto) {
        applicationService.saveTransportDetails(getUser(userDetails), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("${api.url}/admission/applications/sections/9")
    public ResponseEntity<Void> saveSection9(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody DeclarationDTO dto) {
        applicationService.saveDeclarationSection(getUser(userDetails), dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("${api.url}/admission/applications/submit")
    public ResponseEntity<Void> submitApplication(@AuthenticationPrincipal UserDetails userDetails) {
        applicationService.submitApplication(getUser(userDetails));
        return ResponseEntity.ok().build();
    }

    // --- Admin Endpoints ---

    @GetMapping("${api.url}/adm/admission/applications")
    public ResponseEntity<List<ApplicationSummaryDTO>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("${api.url}/adm/admission/applications/{uuid}")
    public ResponseEntity<ApplicationDetailDTO> getApplicationDetail(@PathVariable UUID uuid) {
        return ResponseEntity.ok(applicationService.getApplicationDetail(uuid));
    }

    @PostMapping("${api.url}/adm/admission/applications/{uuid}/approve")
    public ResponseEntity<Void> approveApplication(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails adminPrincipal,
            @Valid @RequestBody AdminApproveRequest request) {
        applicationService.approveApplication(uuid, getUser(adminPrincipal), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("${api.url}/adm/admission/applications/{uuid}/reject")
    public ResponseEntity<Void> rejectApplication(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails adminPrincipal,
            @Valid @RequestBody AdminRejectRequest request) {
        applicationService.rejectApplication(uuid, getUser(adminPrincipal), request);
        return ResponseEntity.ok().build();
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }
}
