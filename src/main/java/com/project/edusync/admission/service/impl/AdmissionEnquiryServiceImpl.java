package com.project.edusync.admission.service.impl;

import com.project.edusync.admission.model.dto.EnquiryCreateRequest;
import com.project.edusync.admission.model.dto.EnquiryReplyRequest;
import com.project.edusync.admission.model.dto.EnquiryResponseDTO;
import com.project.edusync.admission.model.entity.AdmissionEnquiry;
import com.project.edusync.admission.model.enums.EnquiryStatus;
import com.project.edusync.admission.repository.AdmissionEnquiryRepository;
import com.project.edusync.admission.service.AdmissionEnquiryService;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.iam.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdmissionEnquiryServiceImpl implements AdmissionEnquiryService {

    private final AdmissionEnquiryRepository enquiryRepository;

    @Override
    @Transactional
    public EnquiryResponseDTO createEnquiry(User user, EnquiryCreateRequest request) {
        AdmissionEnquiry enquiry = AdmissionEnquiry.builder()
                .user(user)
                .subject(request.getSubject())
                .message(request.getMessage())
                .classApplyingFor(request.getClassApplyingFor())
                .academicYear(request.getAcademicYear())
                .status(EnquiryStatus.PENDING)
                .build();

        AdmissionEnquiry saved = enquiryRepository.save(enquiry);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnquiryResponseDTO> getMyEnquiries(User user) {
        return enquiryRepository.findByUser_Id(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnquiryResponseDTO> getAllEnquiries() {
        return enquiryRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EnquiryResponseDTO replyToEnquiry(UUID uuid, User admin, EnquiryReplyRequest request) {
        AdmissionEnquiry enquiry = enquiryRepository.findByUuid(uuid)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with uuid: " + uuid));

        enquiry.setAdminReply(request.getReply());
        enquiry.setAdminRepliedBy(admin.getUsername());
        enquiry.setAdminRepliedAt(LocalDateTime.now());
        enquiry.setStatus(EnquiryStatus.REPLIED);

        return mapToDTO(enquiryRepository.save(enquiry));
    }

    private EnquiryResponseDTO mapToDTO(AdmissionEnquiry enquiry) {
        return EnquiryResponseDTO.builder()
                .uuid(enquiry.getUuid())
                .applicantName(enquiry.getUser().getUsername()) // Or get fullName if available in UserDetails
                .applicantEmail(enquiry.getUser().getEmail())
                .applicantMobile(enquiry.getUser().getMobile())
                .subject(enquiry.getSubject())
                .message(enquiry.getMessage())
                .adminReply(enquiry.getAdminReply())
                .adminRepliedBy(enquiry.getAdminRepliedBy())
                .adminRepliedAt(enquiry.getAdminRepliedAt())
                .classApplyingFor(enquiry.getClassApplyingFor())
                .academicYear(enquiry.getAcademicYear())
                .status(enquiry.getStatus())
                .createdAt(enquiry.getCreatedAt())
                .build();
    }
}
