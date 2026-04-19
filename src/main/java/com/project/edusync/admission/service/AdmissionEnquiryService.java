package com.project.edusync.admission.service;

import com.project.edusync.admission.model.dto.EnquiryCreateRequest;
import com.project.edusync.admission.model.dto.EnquiryReplyRequest;
import com.project.edusync.admission.model.dto.EnquiryResponseDTO;
import com.project.edusync.iam.model.entity.User;

import java.util.List;
import java.util.UUID;

public interface AdmissionEnquiryService {
    EnquiryResponseDTO createEnquiry(User user, EnquiryCreateRequest request);
    List<EnquiryResponseDTO> getMyEnquiries(User user);
    List<EnquiryResponseDTO> getAllEnquiries();
    EnquiryResponseDTO replyToEnquiry(UUID uuid, User admin, EnquiryReplyRequest request);
}
