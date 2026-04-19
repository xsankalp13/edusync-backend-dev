package com.project.edusync.admission.controller;

import com.project.edusync.admission.model.dto.EnquiryCreateRequest;
import com.project.edusync.admission.model.dto.EnquiryReplyRequest;
import com.project.edusync.admission.model.dto.EnquiryResponseDTO;
import com.project.edusync.admission.service.AdmissionEnquiryService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdmissionEnquiryController {

    private final AdmissionEnquiryService enquiryService;
    private final UserRepository userRepository;

    /**
     * POST /api/v1/admission/enquiries
     * Send an enquiry (Applicant)
     */
    @PostMapping("${api.url}/admission/enquiries")
    public ResponseEntity<EnquiryResponseDTO> createEnquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EnquiryCreateRequest request) {
        
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        EnquiryResponseDTO response = enquiryService.createEnquiry(user, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/admission/enquiries/mine
     * Get applicant's own enquiries
     */
    @GetMapping("${api.url}/admission/enquiries/mine")
    public ResponseEntity<List<EnquiryResponseDTO>> getMyEnquiries(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        List<EnquiryResponseDTO> response = enquiryService.getMyEnquiries(user);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/adm/admission/enquiries
     * View all enquiries (Admin)
     */
    @GetMapping("${api.url}/adm/admission/enquiries")
    public ResponseEntity<List<EnquiryResponseDTO>> getAllEnquiries() {
        List<EnquiryResponseDTO> response = enquiryService.getAllEnquiries();
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/adm/admission/enquiries/{uuid}/reply
     * Reply to an enquiry (Admin)
     */
    @PostMapping("${api.url}/adm/admission/enquiries/{uuid}/reply")
    public ResponseEntity<EnquiryResponseDTO> replyToEnquiry(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails adminPrincipal,
            @Valid @RequestBody EnquiryReplyRequest request) {
        
        User admin = userRepository.findByUsername(adminPrincipal.getUsername()).orElseThrow();
        EnquiryResponseDTO response = enquiryService.replyToEnquiry(uuid, admin, request);
        return ResponseEntity.ok(response);
    }
}
