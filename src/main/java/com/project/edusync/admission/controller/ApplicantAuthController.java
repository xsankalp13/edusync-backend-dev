package com.project.edusync.admission.controller;

import com.project.edusync.admission.model.dto.ApplicantSignupRequest;
import com.project.edusync.admission.service.AdmissionSignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicantAuthController {

    private final AdmissionSignupService signupService;

    /**
     * POST /api/v1/admission/signup
     * Public self-signup for prospective students/parents.
     */
    @PostMapping("${api.url}/admission/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody ApplicantSignupRequest request) {
        signupService.signup(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
