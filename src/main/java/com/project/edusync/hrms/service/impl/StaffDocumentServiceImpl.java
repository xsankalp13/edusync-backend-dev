package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.document.*;
import com.project.edusync.hrms.model.entity.StaffDocument;
import com.project.edusync.hrms.repository.StaffDocumentRepository;
import com.project.edusync.hrms.service.StaffDocumentService;
import com.project.edusync.uis.config.MediaUploadProperties;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffDocumentServiceImpl implements StaffDocumentService {

    private final StaffDocumentRepository documentRepository;
    private final StaffRepository staffRepository;
    private final MediaUploadProperties mediaUploadProperties;

    @Override
    @Transactional
    public DocumentUploadInitResponseDTO initiateUpload(String staffRef, DocumentUploadInitRequestDTO dto) {
        Staff staff = resolveStaff(staffRef);
        String objectKey = "hrms-docs/" + staff.getUuid() + "/" + UUID.randomUUID() + "_" + dto.getFileName();
        Instant expiresAt = Instant.now().plusSeconds(mediaUploadProperties.getUploadInitTtlSeconds());
        String provider = normalizeProvider(mediaUploadProperties.getProvider());

        if ("cloudinary".equals(provider)) {
            MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String folder = "hrms-docs";
            String signatureBase = "folder=" + folder + "&public_id=" + objectKey + "&timestamp=" + timestamp;
            String signature = sha1Hex(signatureBase + cfg.getApiSecret());
            Map<String, String> fields = new HashMap<>();
            fields.put("api_key", cfg.getApiKey());
            fields.put("timestamp", timestamp);
            fields.put("signature", signature);
            fields.put("folder", folder);
            fields.put("public_id", objectKey);
            return DocumentUploadInitResponseDTO.builder()
                    .provider("cloudinary").method("POST")
                    .uploadUrl("https://api.cloudinary.com/v1_1/" + cfg.getCloudName() + "/raw/upload")
                    .objectKey(objectKey).expiresAt(expiresAt).fields(fields).headers(Map.of()).build();
        }

        if ("s3".equals(provider)) {
            String template = mediaUploadProperties.getS3().getUploadUrlTemplate();
            if (!StringUtils.hasText(template))
                throw new EdusyncException("S3 upload URL template not configured.", HttpStatus.INTERNAL_SERVER_ERROR);
            return DocumentUploadInitResponseDTO.builder()
                    .provider("s3").method("PUT")
                    .uploadUrl(template.replace("{objectKey}", objectKey))
                    .objectKey(objectKey).expiresAt(expiresAt).fields(Map.of())
                    .headers(Map.of("Content-Type", dto.getContentType())).build();
        }
        throw new EdusyncException("Unsupported media provider: " + provider, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    @Transactional
    public StaffDocumentResponseDTO confirmUpload(String staffRef, DocumentUploadConfirmRequestDTO dto) {
        Staff staff = resolveStaff(staffRef);
        String expectedPrefix = "hrms-docs/" + staff.getUuid() + "/";
        if (!dto.getObjectKey().startsWith(expectedPrefix))
            throw new EdusyncException("Invalid objectKey for this staff.", HttpStatus.FORBIDDEN);

        StaffDocument doc = new StaffDocument();
        doc.setStaff(staff);
        doc.setCategory(dto.getCategory());
        doc.setDisplayName(dto.getDisplayName());
        doc.setOriginalFileName(dto.getOriginalFileName());
        doc.setObjectKey(dto.getObjectKey());
        doc.setStorageUrl(dto.getStorageUrl());
        doc.setContentType(dto.getContentType());
        doc.setSizeBytes(dto.getSizeBytes());
        doc.setUploadedAt(LocalDateTime.now());
        doc.setExpiryDate(dto.getExpiryDate());
        doc.setActive(true);
        return toResponse(documentRepository.save(doc));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffDocumentResponseDTO> listDocuments(String staffRef) {
        Staff staff = resolveStaff(staffRef);
        return documentRepository.findByStaff_IdAndActiveTrue(staff.getId()).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDocumentResponseDTO getDocument(String staffRef, UUID docUuid) {
        Staff staff = resolveStaff(staffRef);
        StaffDocument doc = documentRepository.findByStaff_IdAndUuidAndActiveTrue(staff.getId(), docUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docUuid));
        return toResponse(doc);
    }

    @Override
    @Transactional
    public void deleteDocument(String staffRef, UUID docUuid) {
        Staff staff = resolveStaff(staffRef);
        StaffDocument doc = documentRepository.findByStaff_IdAndUuidAndActiveTrue(staff.getId(), docUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docUuid));
        doc.setActive(false);
        documentRepository.save(doc);
    }

    private Staff resolveStaff(String staffRef) {
        return PublicIdentifierResolver.resolve(staffRef,
                staffRepository::findByUuid,
                staffRepository::findById,
                "Staff");
    }

    private StaffDocumentResponseDTO toResponse(StaffDocument doc) {
        return new StaffDocumentResponseDTO(doc.getUuid(), doc.getStaff().getUuid(), doc.getCategory(),
                doc.getDisplayName(), doc.getOriginalFileName(), doc.getStorageUrl(),
                doc.getContentType(), doc.getSizeBytes(), doc.getUploadedAt(), doc.getExpiryDate());
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "cloudinary" : provider.trim().toLowerCase();
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new EdusyncException("Signature generation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

