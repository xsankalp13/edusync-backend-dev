package com.project.edusync.adm.service;

import com.project.edusync.adm.model.dto.PickupRequestGenerateDto;
import com.project.edusync.adm.model.dto.PickupRequestResponseDto;
import com.project.edusync.adm.model.dto.PickupVerifyDto;
import com.project.edusync.adm.model.entity.PickupRequest;
import com.project.edusync.adm.model.entity.PickupStatus;
import com.project.edusync.adm.repository.PickupRequestRepository;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PickupRequestService {

    private final PickupRequestRepository pickupRequestRepository;
    private final StudentRepository studentRepository;
    private final AuthUtil authUtil;

    @Transactional
    public PickupRequestResponseDto generatePickup(PickupRequestGenerateDto dto) {
        User currentUser = authUtil.getCurrentUser();
        
        Student student = null;
        if (currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_STUDENT"))) {
            student = studentRepository.findByUserProfile_User_Id(currentUser.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found for current user"));
        } else if (dto.getStudentId() != null) {
            student = studentRepository.findById(dto.getStudentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found by ID"));
        } else if (dto.getStudentUuid() != null && !dto.getStudentUuid().isEmpty()) {
            student = studentRepository.findByUuid(UUID.fromString(dto.getStudentUuid()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found by UUID"));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student identifier is required");
        }

        PickupRequest request = new PickupRequest();
        request.setStudent(student);
        request.setGeneratedBy(currentUser);
        request.setQrToken(UUID.randomUUID().toString());
        request.setStatus(PickupStatus.GENERATED);
        // Expire in 3 hours
        request.setExpiresAt(LocalDateTime.now().plusHours(3));

        pickupRequestRepository.save(request);

        return mapToDto(request, true);
    }

    @Transactional
    public PickupRequestResponseDto verifyPickup(PickupVerifyDto dto) {
        User guardUser = authUtil.getCurrentUser();

        PickupRequest request = pickupRequestRepository.findByQrToken(dto.getQrToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid QR Code"));

        if (request.getStatus() == PickupStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR Code already used");
        }

        if (request.getStatus() == PickupStatus.EXPIRED || LocalDateTime.now().isAfter(request.getExpiresAt())) {
            request.setStatus(PickupStatus.EXPIRED);
            pickupRequestRepository.save(request);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR Code expired");
        }

        request.setStatus(PickupStatus.VERIFIED);
        request.setVerifiedBy(guardUser);
        request.setVerifiedAt(LocalDateTime.now());
        pickupRequestRepository.save(request);

        return mapToDto(request, false);
    }

    @Transactional(readOnly = true)
    public List<PickupRequestResponseDto> getMyGenerateHistory() {
        User currentUser = authUtil.getCurrentUser();
        return pickupRequestRepository.findByGeneratedBy_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(r -> {
                    // check if expired implicitly
                    if (r.getStatus() == PickupStatus.GENERATED && LocalDateTime.now().isAfter(r.getExpiresAt())) {
                        r.setStatus(PickupStatus.EXPIRED);
                        // We are in readOnly transaction, we might not save it here natively, but UI gets EXPIRED
                    }
                    return mapToDto(r, true);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PickupRequestResponseDto> getAllPickups() {
        return pickupRequestRepository.findAll()
                .stream()
                .map(r -> mapToDto(r, false)) // no need to return raw tokens to admins
                .collect(Collectors.toList());
    }

    private PickupRequestResponseDto mapToDto(PickupRequest r, boolean includeToken) {
        Student s = r.getStudent();
        String studentName = "";
        if (s.getUserProfile() != null) {
            studentName = s.getUserProfile().getFirstName() + " " + s.getUserProfile().getLastName();
        }
        
        String classInfo = "";
        if (s.getSection() != null) {
            classInfo = s.getSection().getSectionName();
        }

        String generatedByName = "";
        if (r.getGeneratedBy() != null) {
            generatedByName = r.getGeneratedBy().getUsername();
        }
        
        PickupStatus effectiveStatus = r.getStatus();
        if (effectiveStatus == PickupStatus.GENERATED && LocalDateTime.now().isAfter(r.getExpiresAt())) {
            effectiveStatus = PickupStatus.EXPIRED;
        }

        return PickupRequestResponseDto.builder()
                .uuid(r.getUuid() != null ? r.getUuid().toString() : "")
                .studentUuid(s.getUuid() != null ? s.getUuid().toString() : "")
                .studentName(studentName)
                .studentClassInfo(classInfo)
                .generatedByName(generatedByName)
                .status(effectiveStatus)
                .qrToken(includeToken && effectiveStatus == PickupStatus.GENERATED ? r.getQrToken() : null)
                .generatedAt(r.getCreatedAt())
                .expiresAt(r.getExpiresAt())
                .verifiedAt(r.getVerifiedAt())
                .build();
    }
}
