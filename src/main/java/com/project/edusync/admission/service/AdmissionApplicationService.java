package com.project.edusync.admission.service;

import com.project.edusync.admission.model.dto.*;
import com.project.edusync.admission.model.entity.AdmissionApplication;
import com.project.edusync.iam.model.entity.User;

import java.util.List;
import java.util.UUID;

public interface AdmissionApplicationService {
    AdmissionApplication createOrGetApplication(User user);
    AdmissionApplication getApplicationByUuid(UUID uuid);
    ApplicationDetailDTO getMyApplication(User user);
    
    // Section saving methods
    void saveStudentBasicDetails(User user, StudentBasicDetailsDTO dto);
    void saveAddressContactDetails(User user, AddressContactDTO dto);
    void saveParentGuardianDetails(User user, ParentGuardianDTO dto);
    void saveAcademicInformation(User user, AcademicInfoDTO dto);
    void saveDocumentUploads(User user, DocumentUploadsDTO dto);
    void saveAdmissionDetails(User user, AdmissionDetailsDTO dto);
    void saveMedicalInformation(User user, MedicalInfoDTO dto);
    void saveTransportDetails(User user, TransportDetailsDTO dto);
    void saveDeclarationSection(User user, DeclarationDTO dto);
    
    void submitApplication(User user);
    
    // Admin methods
    List<ApplicationSummaryDTO> getAllApplications();
    ApplicationDetailDTO getApplicationDetail(UUID uuid);
    void approveApplication(UUID uuid, User admin, AdminApproveRequest request);
    void rejectApplication(UUID uuid, User admin, AdminRejectRequest request);
}
