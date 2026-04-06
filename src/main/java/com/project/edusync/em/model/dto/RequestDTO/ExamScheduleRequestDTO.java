package com.project.edusync.em.model.dto.RequestDTO;

import com.project.edusync.em.model.enums.SeatSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for creating or updating an ExamSchedule.
 * Uses UUIDs for external references to ensure API security and decoupling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamScheduleRequestDTO {

    @NotNull(message = "Duration (minutes) is required")
    private Integer duration;
    @NotNull(message = "Class UUID is required")
    private UUID classId;

    private UUID sectionId; // Optional, as an exam might be for the whole class

    @NotNull(message = "Subject UUID is required")
    private UUID subjectId;

    @NotNull(message = "Exam date is required")
    private LocalDate examDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Max marks are required")
    @DecimalMin(value = "1.0", message = "Max marks must be at least 1.0")
    private BigDecimal maxMarks;

    @NotNull(message = "Passing marks are required")
    @DecimalMin(value = "0.0", message = "Passing marks cannot be negative")
    private BigDecimal passingMarks;

    @Size(max = 50, message = "Room number must be under 50 characters")
    private String roomNumber;

    private Integer maxStudentsPerSeat;
    private SeatSide seatSide;

    public Integer getDuration() { return duration; }
    public Integer getMaxStudentsPerSeat() { return maxStudentsPerSeat; }
    public void setMaxStudentsPerSeat(Integer maxStudentsPerSeat) { this.maxStudentsPerSeat = maxStudentsPerSeat; }
}