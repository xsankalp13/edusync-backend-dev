package com.project.edusync.hrms.dto.calendar;

import java.util.List;

public record BulkOperationResultDTO(
        int totalProcessed,
        int successCount,
        int failedCount,
        List<String> errors
) {
}

