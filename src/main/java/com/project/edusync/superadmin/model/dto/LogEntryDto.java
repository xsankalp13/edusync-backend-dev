package com.project.edusync.superadmin.model.dto;

public record LogEntryDto(
        String timestamp,
        String level,
        String logger,
        String thread,
        String message
) {
}

