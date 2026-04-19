package com.project.edusync.ams.model.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record StaffDailyStatsResponseDTO(
        LocalDate date,
        long totalMarked,
        long present,
        long absent,
        long late,
        long halfDay,
        long onLeave,
        long unmarkedCount
) {
}

