package com.project.edusync.uis.controller;

import com.project.edusync.uis.model.dto.profile.BulkUploadReportDTO;
import com.project.edusync.uis.service.BulkPhotoUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${api.url}/auth/admin/bulk-upload")
@RequiredArgsConstructor
@Tag(name = "Bulk Upload", description = "Endpoints for bulk operations")
public class BulkPhotoUploadController {

    private final BulkPhotoUploadService bulkPhotoUploadService;

    @PostMapping("/photos/{userType}")
    @PreAuthorize("hasAnyAuthority('ROLE_SCHOOL_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Bulk Upload Profile Photos", 
               description = "Accepts a ZIP file containing profile photos. Filenames must match enrollmentNumber or employeeId.")
    public ResponseEntity<BulkUploadReportDTO> uploadBulkPhotos(
            @PathVariable String userType,
            @RequestParam("file") MultipartFile file) {
        
        BulkUploadReportDTO report = bulkPhotoUploadService.uploadBulkPhotos(userType, file);
        return ResponseEntity.ok(report);
    }
}
