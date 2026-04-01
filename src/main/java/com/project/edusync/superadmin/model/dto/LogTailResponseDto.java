package com.project.edusync.superadmin.model.dto;

import java.util.List;

public record LogTailResponseDto(
        String logFile,
        int totalLinesReturned,
        List<LogEntryDto> entries
) {
}

