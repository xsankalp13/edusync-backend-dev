package com.project.edusync.uis.model.dto.dashboard;

/**
 * Backward-compatible wrapper. New integrations should use IntelligenceResponseDTO directly.
 */
@Deprecated(forRemoval = false)
public record StudentDashboardResponseDTO(IntelligenceResponseDTO intelligence) {
}

