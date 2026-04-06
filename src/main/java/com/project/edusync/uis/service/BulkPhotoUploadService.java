package com.project.edusync.uis.service;

import com.project.edusync.uis.model.dto.profile.BulkUploadReportDTO;
import org.springframework.web.multipart.MultipartFile;

public interface BulkPhotoUploadService {

    /**
     * Processes a ZIP file containing profile photos for bulk upload.
     * The filenames should exactly match the student enrollment number or staff system ID.
     * 
     * @param userType Either "students" or "staff"
     * @param zipFile The uploaded ZIP file containing .jpg/.png images
     * @return BulkUploadReportDTO with success/failure counts and error details
     */
    BulkUploadReportDTO uploadBulkPhotos(String userType, MultipartFile zipFile);
}
