package com.project.edusync.enrollment.model.dto;

import java.util.List;

public record BulkRoomImportReportDTO(
        String status,
        int totalRows,
        int successCount,
        int failureCount,
        List<String> errorMessages
) {
}

