package com.project.edusync.em.model.dto.ResponseDTO;
import com.project.edusync.em.model.enums.StudentAttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for sending StudentMark data back to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentMarkResponseDTO {

    private UUID uuid;
    private Long scheduleId;
    private Long studentId;
    private BigDecimal marksObtained;
    private StudentAttendanceStatus attendanceStatus;
    private String grade; // This is calculated and set by the service
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}