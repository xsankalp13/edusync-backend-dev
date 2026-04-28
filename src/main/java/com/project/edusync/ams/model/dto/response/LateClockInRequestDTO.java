package com.project.edusync.ams.model.dto.response;

import com.project.edusync.ams.model.enums.LateClockInStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class LateClockInRequestDTO {
    private UUID uuid;
    private Long staffId;
    private String staffName;
    private String employeeId;
    private String designation;
    private LocalDate attendanceDate;
    private LocalTime clockInTime;
    private Integer minutesLate;
    private String reason;
    private LateClockInStatus status;
    private String adminRemarks;
    private ZonedDateTime createdAt;
}
