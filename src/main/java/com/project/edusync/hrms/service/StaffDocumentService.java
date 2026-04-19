package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.document.*;

import java.util.List;
import java.util.UUID;

public interface StaffDocumentService {
    DocumentUploadInitResponseDTO initiateUpload(String staffRef, DocumentUploadInitRequestDTO dto);
    StaffDocumentResponseDTO confirmUpload(String staffRef, DocumentUploadConfirmRequestDTO dto);
    List<StaffDocumentResponseDTO> listDocuments(String staffRef);
    StaffDocumentResponseDTO getDocument(String staffRef, UUID docUuid);
    void deleteDocument(String staffRef, UUID docUuid);
}

