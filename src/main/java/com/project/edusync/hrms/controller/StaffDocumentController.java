package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.document.*;
import com.project.edusync.hrms.service.StaffDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/hrms/staff/{staffRef}/documents")
@RequiredArgsConstructor
@Tag(name = "HRMS Staff Documents", description = "Staff document management")
public class StaffDocumentController {

    private final StaffDocumentService staffDocumentService;

    @PostMapping("/upload-url")
    @Operation(summary = "Initiate document upload")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<DocumentUploadInitResponseDTO> initiateUpload(
            @PathVariable String staffRef,
            @Valid @RequestBody DocumentUploadInitRequestDTO dto) {
        return ResponseEntity.ok(staffDocumentService.initiateUpload(staffRef, dto));
    }

    @PostMapping("/confirm-upload")
    @Operation(summary = "Confirm document upload")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffDocumentResponseDTO> confirmUpload(
            @PathVariable String staffRef,
            @Valid @RequestBody DocumentUploadConfirmRequestDTO dto) {
        return ResponseEntity.ok(staffDocumentService.confirmUpload(staffRef, dto));
    }

    @GetMapping
    @Operation(summary = "List documents for staff")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<StaffDocumentResponseDTO>> list(@PathVariable String staffRef) {
        return ResponseEntity.ok(staffDocumentService.listDocuments(staffRef));
    }

    @GetMapping("/{docUuid}")
    @Operation(summary = "Get a specific document")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffDocumentResponseDTO> get(
            @PathVariable String staffRef,
            @PathVariable UUID docUuid) {
        return ResponseEntity.ok(staffDocumentService.getDocument(staffRef, docUuid));
    }

    @DeleteMapping("/{docUuid}")
    @Operation(summary = "Soft-delete a document")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable String staffRef,
            @PathVariable UUID docUuid) {
        staffDocumentService.deleteDocument(staffRef, docUuid);
        return ResponseEntity.noContent().build();
    }
}

