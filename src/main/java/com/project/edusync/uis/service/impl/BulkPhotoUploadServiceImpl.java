package com.project.edusync.uis.service.impl;

import com.project.edusync.common.exception.uis.BulkUploadException;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.config.MediaUploadProperties;
import com.project.edusync.uis.model.dto.profile.BulkUploadReportDTO;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.repository.UserProfileRepository;
import com.project.edusync.uis.service.BulkPhotoUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkPhotoUploadServiceImpl implements BulkPhotoUploadService {

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final UserProfileRepository userProfileRepository;
    private final MediaUploadProperties mediaUploadProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public BulkUploadReportDTO uploadBulkPhotos(String userType, MultipartFile zipFile) {
        if (zipFile.isEmpty()) {
            throw new BulkUploadException("ZIP file is empty", HttpStatus.BAD_REQUEST);
        }

        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
        if ("cloudinary".equalsIgnoreCase(mediaUploadProperties.getProvider())) {
            if (!StringUtils.hasText(cfg.getCloudName()) || !StringUtils.hasText(cfg.getApiKey()) || !StringUtils.hasText(cfg.getApiSecret())) {
                throw new BulkUploadException("Cloudinary is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            throw new BulkUploadException("Bulk upload currently only supports Cloudinary", HttpStatus.NOT_IMPLEMENTED);
        }

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String filename = entry.getName();
                filename = filename.substring(filename.lastIndexOf('/') + 1); // remove directories
                if (filename.startsWith(".")) {
                    continue; // ignore hidden files like .DS_Store
                }

                String ext = getExtension(filename).toLowerCase();
                if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png")) {
                    failedCount++;
                    errors.add(filename + ": Unsupported file extension");
                    continue;
                }

                String identifier = getFilenameWithoutExtension(filename);

                try {
                    byte[] fileBytes = zis.readAllBytes();
                    long fileSize = fileBytes.length;
                    
                    if (fileSize > mediaUploadProperties.getMaxFileSizeBytes()) {
                        failedCount++;
                        errors.add(filename + ": File too large (max " + (mediaUploadProperties.getMaxFileSizeBytes() / 1024 / 1024) + "MB)");
                        continue;
                    }

                    UserProfile profile = findUserProfile(userType, identifier);
                    if (profile == null) {
                        failedCount++;
                        errors.add(filename + ": User '" + identifier + "' not found");
                        continue;
                    }

                    // Proceed to upload to Cloudinary directly
                    String objectKey = cfg.getFolder() + "/" + profile.getUser().getId() + "_" + UUID.randomUUID().toString();
                    String uploadUrl = "https://api.cloudinary.com/v1_1/" + cfg.getCloudName() + "/image/upload";
                    String timestamp = String.valueOf(Instant.now().getEpochSecond());

                    String signatureBase = "folder=" + cfg.getFolder() + "&public_id=" + objectKey + "&timestamp=" + timestamp;
                    String signature = sha1Hex(signatureBase + cfg.getApiSecret());

                    String base64Prefix = "jpeg".equals(ext) ? "jpg" : ext;
                    String base64DataUri = "data:image/" + base64Prefix + ";base64," + Base64.getEncoder().encodeToString(fileBytes);

                    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                    body.add("file", base64DataUri);
                    body.add("api_key", cfg.getApiKey());
                    body.add("timestamp", timestamp);
                    body.add("signature", signature);
                    body.add("folder", cfg.getFolder());
                    body.add("public_id", objectKey);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, requestEntity, Map.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        String secureUrl = (String) response.getBody().get("secure_url");
                        profile.setProfileUrl(secureUrl);
                        userProfileRepository.save(profile);
                        successCount++;
                    } else {
                        failedCount++;
                        errors.add(filename + ": Failed to upload to Cloudinary");
                    }

                } catch (Exception e) {
                    log.error("Failed to process file {}", filename, e);
                    failedCount++;
                    errors.add(filename + ": Error processing file - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract or process ZIP file", e);
            throw new BulkUploadException("Failed to read ZIP file: " + e.getMessage(), HttpStatus.BAD_REQUEST, e);
        }

        return BulkUploadReportDTO.builder()
                .success(successCount)
                .failed(failedCount)
                .errors(errors)
                .build();
    }

    private UserProfile findUserProfile(String userType, String identifier) {
        if ("students".equalsIgnoreCase(userType)) {
            return studentRepository.findByEnrollmentNumber(identifier)
                    .map(Student::getUserProfile)
                    .orElse(null);
        } else if ("staff".equalsIgnoreCase(userType)) {
            return staffRepository.findByEmployeeId(identifier)
                    .map(Staff::getUserProfile)
                    .orElse(null);
        }
        return null;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    private String getFilenameWithoutExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BulkUploadException("SHA-1 algorithm not found", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
