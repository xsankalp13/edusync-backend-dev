package com.project.edusync.admission.service;

import com.project.edusync.admission.model.dto.ApplicantSignupRequest;
import com.project.edusync.iam.model.entity.User;

public interface AdmissionSignupService {
    User signup(ApplicantSignupRequest request);
}
