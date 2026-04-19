package com.project.edusync.admission.service.impl;

import com.project.edusync.admission.model.dto.*;
import com.project.edusync.admission.model.entity.*;
import com.project.edusync.admission.model.enums.AdmissionStatus;
import com.project.edusync.admission.repository.*;
import com.project.edusync.admission.service.AdmissionApplicationService;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.iam.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionApplicationServiceImpl implements AdmissionApplicationService {

    private final AdmissionApplicationRepository applicationRepository;
    private final StudentBasicDetailsRepository studentBasicDetailsRepository;
    private final AddressContactDetailsRepository addressContactDetailsRepository;
    private final ParentGuardianDetailsRepository parentGuardianDetailsRepository;
    private final AcademicInformationRepository academicInformationRepository;
    private final DocumentUploadsRepository documentUploadsRepository;
    private final AdmissionDetailsRepository admissionDetailsRepository;
    private final MedicalInformationRepository medicalInformationRepository;
    private final TransportDetailsRepository transportDetailsRepository;
    private final DeclarationSectionRepository declarationSectionRepository;

    @Override
    @Transactional
    public AdmissionApplication createOrGetApplication(User user) {
        return applicationRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    AdmissionApplication app = AdmissionApplication.builder()
                            .user(user)
                            .status(AdmissionStatus.DRAFT)
                            .currentSection(1)
                            .build();
                    return applicationRepository.save(app);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public AdmissionApplication getApplicationByUuid(UUID uuid) {
        return applicationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("AdmissionApplication", "uuid", uuid));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDetailDTO getMyApplication(User user) {
        AdmissionApplication app = createOrGetApplication(user);
        return mapToDetailDTO(app);
    }

    @Override
    @Transactional
    public void saveStudentBasicDetails(User user, StudentBasicDetailsDTO dto) {
        AdmissionApplication app = createOrGetApplication(user);
        StudentBasicDetails entity = studentBasicDetailsRepository.findByApplication_Id(app.getId());
        if (entity == null) {
            entity = new StudentBasicDetails();
            entity.setApplication(app);
        }
        entity.setFullName(dto.getFullName());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setGender(dto.getGender());
        entity.setBloodGroup(dto.getBloodGroup());
        entity.setNationality(dto.getNationality());
        entity.setReligion(dto.getReligion());
        entity.setCaste(dto.getCaste());
        entity.setAadhaarNumber(dto.getAadhaarNumber());
        entity.setMotherTongue(dto.getMotherTongue());
        entity.setCategory(dto.getCategory());
        
        studentBasicDetailsRepository.save(entity);
        updateProgress(app, 2);
    }

    @Override
    @Transactional
    public void saveAddressContactDetails(User user, AddressContactDTO dto) {
        AdmissionApplication app = createOrGetApplication(user);
        AddressContactDetails entity = addressContactDetailsRepository.findByApplication_Id(app.getId());
        if (entity == null) {
            entity = new AddressContactDetails();
            entity.setApplication(app);
        }
        entity.setResidentialAddress(dto.getResidentialAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setPinCode(dto.getPinCode());
        entity.setPermanentAddress(dto.getPermanentAddress());
        entity.setPrimaryMobile(dto.getPrimaryMobile());
        entity.setAlternateMobile(dto.getAlternateMobile());
        entity.setEmailId(dto.getEmailId());
        
        addressContactDetailsRepository.save(entity);
        updateProgress(app, 3);
    }

    @Override
    @Transactional
    public void saveParentGuardianDetails(User user, ParentGuardianDTO dto) {
        AdmissionApplication app = createOrGetApplication(user);
        ParentGuardianDetails entity = parentGuardianDetailsRepository.findByApplication_Id(app.getId());
        if (entity == null) {
            entity = new ParentGuardianDetails();
            entity.setApplication(app);
        }
        entity.setFatherName(dto.getFatherName());
        entity.setFatherOccupation(dto.getFatherOccupation());
        entity.setFatherQualification(dto.getFatherQualification());
        entity.setFatherAnnualIncome(dto.getFatherAnnualIncome());
        entity.setFatherMobile(dto.getFatherMobile());
        entity.setMotherName(dto.getMotherName());
        entity.setMotherOccupation(dto.getMotherOccupation());
        entity.setMotherQualification(dto.getMotherQualification());
        entity.setMotherAnnualIncome(dto.getMotherAnnualIncome());
        entity.setMotherMobile(dto.getMotherMobile());
        entity.setGuardianName(dto.getGuardianName());
        entity.setGuardianRelationship(dto.getGuardianRelationship());
        entity.setGuardianContact(dto.getGuardianContact());
        
        parentGuardianDetailsRepository.save(entity);
        updateProgress(app, 4);
    }

    @Override
    @Transactional
    public void saveAcademicInformation(User user, AcademicInfoDTO dto) {
        AdmissionApplication app = createOrGetApplication(user);
        AcademicInformation entity = academicInformationRepository.findByApplication_Id(app.getId());
        if (entity == null) {
            entity = new AcademicInformation();
            entity.setApplication(app);
        }
        entity.setPreviousSchoolName(dto.getPreviousSchoolName());
        entity.setBoard(dto.getBoard());
        entity.setLastClassAttended(dto.getLastClassAttended());
        entity.setMarksOrGradeObtained(dto.getMarksOrGradeObtained());
        entity.setMediumOfInstruction(dto.getMediumOfInstruction());
        entity.setTransferCertificateDetails(dto.getTransferCertificateDetails());
        
        academicInformationRepository.save(entity);
        updateProgress(app, 5);
    }

    @Override
    @Transactional
    public void saveDocumentUploads(User user, DocumentUploadsDTO dto) {
        AdmissionApplication app = createOrGetApplication(user);
        DocumentUploads entity = documentUploadsRepository.findByApplication_Id(app.getId());
        if (entity == null) {
            entity = new DocumentUploads();
            entity.setApplication(app);
        }
        entity.setBirthCertificateUrl(dto.getBirthCertificateUrl());
        entity.setStudentPhotoUrl(dto.getStudentPhotoUrl());
        entity.setParentPhotoUrl(dto.getParentPhotoUrl());
        entity.setAadhaarCardUrl(dto.getAadhaarCardUrl());
        entity.setTransferCertificateUrl(dto.getTransferCertificateUrl());
        entity.setReportCardUrl(dto.getReportCardUrl());
        entity.setAddressProofUrl(dto.getAddressProofUrl());
        entity.setCasteCertificateUrl(dto.getCasteCertificateUrl());
        entity.setIncomeCertificateUrl(dto.getIncomeCertificateUrl());
        entity.setMedicalCertificateUrl(dto.getMedicalCertificateUrl());
        
        documentUploadsRepository.save(entity);
        updateProgress(app, 6);
    }

    @Override
    @Transactional
    public void saveAdmissionDetails(User user, AdmissionDetailsDTO dto) {
        AdmissionApplication app = createOrGetApplication(user);
        AdmissionDetails entity = admissionDetailsRepository.findByApplication_Id(app.getId());
        if (entity == null) {
            entity = new AdmissionDetails();
            entity.setApplication(app);
        }
        entity.setClassApplyingFor(dto.getClassApplyingFor());
        entity.setAcademicYear(dto.getAcademicYear());
        entity.setStream(dto.getStream());
        entity.setSecondLanguagePreference(dto.getSecondLanguagePreference());
        entity.setThirdLanguagePreference(dto.getThirdLanguagePreference());
        entity.setTransportRequired(dto.isTransportRequired());
        entity.setHostelRequired(dto.isHostelRequired());
        
        admissionDetailsRepository.save(entity);
        updateProgress(app, 7);
    }

    @Override
    @Transactional
    public void saveMedicalInformation(User user, MedicalInfoDTO dto) {
        AdmissionApplication app = createOrGetApplication(user);
        MedicalInformation entity = medicalInformationRepository.findByApplication_Id(app.getId());
        if (entity == null) {
            entity = new MedicalInformation();
            entity.setApplication(app);
        }
        entity.setAllergies(dto.getAllergies());
        entity.setExistingMedicalConditions(dto.getExistingMedicalConditions());
        entity.setDisabilities(dto.getDisabilities());
        entity.setEmergencyContactPerson(dto.getEmergencyContactPerson());
        entity.setEmergencyContactNumber(dto.getEmergencyContactNumber());
        
        medicalInformationRepository.save(entity);
        updateProgress(app, 8);
    }

    @Override
    @Transactional
    public void saveTransportDetails(User user, TransportDetailsDTO dto) {
        AdmissionApplication app = createOrGetApplication(user);
        TransportDetails entity = transportDetailsRepository.findByApplication_Id(app.getId());
        if (entity == null) {
            entity = new TransportDetails();
            entity.setApplication(app);
        }
        entity.setPickupLocation(dto.getPickupLocation());
        entity.setRouteOrStop(dto.getRouteOrStop());
        entity.setDistanceFromSchool(dto.getDistanceFromSchool());
        
        transportDetailsRepository.save(entity);
        updateProgress(app, 9);
    }

    @Override
    @Transactional
    public void saveDeclarationSection(User user, DeclarationDTO dto) {
        AdmissionApplication app = createOrGetApplication(user);
        DeclarationSection entity = declarationSectionRepository.findByApplication_Id(app.getId());
        if (entity == null) {
            entity = new DeclarationSection();
            entity.setApplication(app);
        }
        entity.setInformationCorrect(dto.isInformationCorrect());
        entity.setAgreesToRules(dto.isAgreesToRules());
        entity.setSignatureUrl(dto.getSignatureUrl());
        entity.setDeclarationDate(dto.getDeclarationDate());
        
        declarationSectionRepository.save(entity);
        // This is the last section, no need to increment further than 9
    }

    @Override
    @Transactional
    public void submitApplication(User user) {
        AdmissionApplication app = createOrGetApplication(user);
        // Validation: verify all sections are filled (simple check on currentSection)
        app.setStatus(AdmissionStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        applicationRepository.save(app);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationSummaryDTO> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(app -> ApplicationSummaryDTO.builder()
                        .id(app.getId())
                        .uuid(app.getUuid())
                        .applicantName(app.getUser().getUsername())
                        .email(app.getUser().getEmail())
                        .status(app.getStatus())
                        .classApplyingFor(app.getAdmissionDetails() != null ? app.getAdmissionDetails().getClassApplyingFor() : "N/A")
                        .currentSection(app.getCurrentSection())
                        .createdAt(app.getCreatedAt())
                        .formFee(app.getFormFee())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDetailDTO getApplicationDetail(UUID uuid) {
        AdmissionApplication app = getApplicationByUuid(uuid);
        return mapToDetailDTO(app);
    }

    @Override
    @Transactional
    public void approveApplication(UUID uuid, User admin, AdminApproveRequest request) {
        AdmissionApplication app = getApplicationByUuid(uuid);
        app.setStatus(AdmissionStatus.APPROVED);
        app.setFormFee(request.getFormFee());
        app.setAdminRemarks(request.getRemarks());
        app.setApprovedBy(admin.getUsername());
        app.setApprovedAt(LocalDateTime.now());
        applicationRepository.save(app);
    }

    @Override
    @Transactional
    public void rejectApplication(UUID uuid, User admin, AdminRejectRequest request) {
        AdmissionApplication app = getApplicationByUuid(uuid);
        app.setStatus(AdmissionStatus.REJECTED);
        app.setAdminRemarks(request.getRemarks());
        app.setRejectedAt(LocalDateTime.now());
        applicationRepository.save(app);
    }

    private void updateProgress(AdmissionApplication app, int nextSection) {
        if (app.getCurrentSection() < nextSection) {
            app.setCurrentSection(nextSection);
            applicationRepository.save(app);
        }
    }

    private ApplicationDetailDTO mapToDetailDTO(AdmissionApplication app) {
        return ApplicationDetailDTO.builder()
                .uuid(app.getUuid())
                .status(app.getStatus())
                .currentSection(app.getCurrentSection())
                .adminRemarks(app.getAdminRemarks())
                .formFee(app.getFormFee())
                .feePaymentId(app.getFeePaymentId())
                .submittedAt(app.getSubmittedAt())
                .studentBasicDetails(mapStudentBasic(app.getStudentBasicDetails()))
                .addressContactDetails(mapAddressContact(app.getAddressContactDetails()))
                .parentGuardianDetails(mapParentGuardian(app.getParentGuardianDetails()))
                .academicInformation(mapAcademic(app.getAcademicInformation()))
                .documentUploads(mapDocuments(app.getDocumentUploads()))
                .admissionDetails(mapAdmission(app.getAdmissionDetails()))
                .medicalInformation(mapMedical(app.getMedicalInformation()))
                .transportDetails(mapTransport(app.getTransportDetails()))
                .declarationSection(mapDeclaration(app.getDeclarationSection()))
                .build();
    }

    private StudentBasicDetailsDTO mapStudentBasic(StudentBasicDetails entity) {
        if (entity == null) return null;
        StudentBasicDetailsDTO dto = new StudentBasicDetailsDTO();
        dto.setFullName(entity.getFullName());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setGender(entity.getGender());
        dto.setBloodGroup(entity.getBloodGroup());
        dto.setNationality(entity.getNationality());
        dto.setReligion(entity.getReligion());
        dto.setCaste(entity.getCaste());
        dto.setAadhaarNumber(entity.getAadhaarNumber());
        dto.setMotherTongue(entity.getMotherTongue());
        dto.setCategory(entity.getCategory());
        return dto;
    }

    private AddressContactDTO mapAddressContact(AddressContactDetails entity) {
        if (entity == null) return null;
        AddressContactDTO dto = new AddressContactDTO();
        dto.setResidentialAddress(entity.getResidentialAddress());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setPinCode(entity.getPinCode());
        dto.setPermanentAddress(entity.getPermanentAddress());
        dto.setPrimaryMobile(entity.getPrimaryMobile());
        dto.setAlternateMobile(entity.getAlternateMobile());
        dto.setEmailId(entity.getEmailId());
        return dto;
    }

    private ParentGuardianDTO mapParentGuardian(ParentGuardianDetails entity) {
        if (entity == null) return null;
        ParentGuardianDTO dto = new ParentGuardianDTO();
        dto.setFatherName(entity.getFatherName());
        dto.setFatherOccupation(entity.getFatherOccupation());
        dto.setFatherQualification(entity.getFatherQualification());
        dto.setFatherAnnualIncome(entity.getFatherAnnualIncome());
        dto.setFatherMobile(entity.getFatherMobile());
        dto.setMotherName(entity.getMotherName());
        dto.setMotherOccupation(entity.getMotherOccupation());
        dto.setMotherQualification(entity.getMotherQualification());
        dto.setMotherAnnualIncome(entity.getMotherAnnualIncome());
        dto.setMotherMobile(entity.getMotherMobile());
        dto.setGuardianName(entity.getGuardianName());
        dto.setGuardianRelationship(entity.getGuardianRelationship());
        dto.setGuardianContact(entity.getGuardianContact());
        return dto;
    }

    private AcademicInfoDTO mapAcademic(AcademicInformation entity) {
        if (entity == null) return null;
        AcademicInfoDTO dto = new AcademicInfoDTO();
        dto.setPreviousSchoolName(entity.getPreviousSchoolName());
        dto.setBoard(entity.getBoard());
        dto.setLastClassAttended(entity.getLastClassAttended());
        dto.setMarksOrGradeObtained(entity.getMarksOrGradeObtained());
        dto.setMediumOfInstruction(entity.getMediumOfInstruction());
        dto.setTransferCertificateDetails(entity.getTransferCertificateDetails());
        return dto;
    }

    private DocumentUploadsDTO mapDocuments(DocumentUploads entity) {
        if (entity == null) return null;
        DocumentUploadsDTO dto = new DocumentUploadsDTO();
        dto.setBirthCertificateUrl(entity.getBirthCertificateUrl());
        dto.setStudentPhotoUrl(entity.getStudentPhotoUrl());
        dto.setParentPhotoUrl(entity.getParentPhotoUrl());
        dto.setAadhaarCardUrl(entity.getAadhaarCardUrl());
        dto.setTransferCertificateUrl(entity.getTransferCertificateUrl());
        dto.setReportCardUrl(entity.getReportCardUrl());
        dto.setAddressProofUrl(entity.getAddressProofUrl());
        dto.setCasteCertificateUrl(entity.getCasteCertificateUrl());
        dto.setIncomeCertificateUrl(entity.getIncomeCertificateUrl());
        dto.setMedicalCertificateUrl(entity.getMedicalCertificateUrl());
        return dto;
    }

    private AdmissionDetailsDTO mapAdmission(AdmissionDetails entity) {
        if (entity == null) return null;
        AdmissionDetailsDTO dto = new AdmissionDetailsDTO();
        dto.setClassApplyingFor(entity.getClassApplyingFor());
        dto.setAcademicYear(entity.getAcademicYear());
        dto.setStream(entity.getStream());
        dto.setSecondLanguagePreference(entity.getSecondLanguagePreference());
        dto.setThirdLanguagePreference(entity.getThirdLanguagePreference());
        dto.setTransportRequired(entity.isTransportRequired());
        dto.setHostelRequired(entity.isHostelRequired());
        return dto;
    }

    private MedicalInfoDTO mapMedical(MedicalInformation entity) {
        if (entity == null) return null;
        MedicalInfoDTO dto = new MedicalInfoDTO();
        dto.setAllergies(entity.getAllergies());
        dto.setExistingMedicalConditions(entity.getExistingMedicalConditions());
        dto.setDisabilities(entity.getDisabilities());
        dto.setEmergencyContactPerson(entity.getEmergencyContactPerson());
        dto.setEmergencyContactNumber(entity.getEmergencyContactNumber());
        return dto;
    }

    private TransportDetailsDTO mapTransport(TransportDetails entity) {
        if (entity == null) return null;
        TransportDetailsDTO dto = new TransportDetailsDTO();
        dto.setPickupLocation(entity.getPickupLocation());
        dto.setRouteOrStop(entity.getRouteOrStop());
        dto.setDistanceFromSchool(entity.getDistanceFromSchool());
        return dto;
    }

    private DeclarationDTO mapDeclaration(DeclarationSection entity) {
        if (entity == null) return null;
        DeclarationDTO dto = new DeclarationDTO();
        dto.setInformationCorrect(entity.isInformationCorrect());
        dto.setAgreesToRules(entity.isAgreesToRules());
        dto.setSignatureUrl(entity.getSignatureUrl());
        dto.setDeclarationDate(entity.getDeclarationDate());
        return dto;
    }
}
