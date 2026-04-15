package com.project.edusync.hrms.dto.bank;

import java.util.List;

/**
 * Result summary returned after a bulk CSV import of bank details.
 */
public record BankDetailsBulkImportResultDTO(
        int totalRows,
        int successCount,
        int skippedCount,
        int errorCount,
        List<RowError> errors
) {
    public record RowError(int rowNumber, String employeeId, String reason) {}
}
