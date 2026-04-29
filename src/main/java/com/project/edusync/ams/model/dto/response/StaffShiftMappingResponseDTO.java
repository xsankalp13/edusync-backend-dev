package com.project.edusync.ams.model.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record StaffShiftMappingResponseDTO(
        Long mappingId,
        String uuid,
        String staffUuid,
        String staffName,
        String employeeId,
        String staffCategory,
        String shiftUuid,
        String shiftName,
        LocalTime shiftStartTime,
        LocalTime shiftEndTime,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        java.util.List<Integer> applicableDays
) {
}

