package com.project.edusync.em.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Seat allocation result — returned after allocation and for allocation listing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAllocationResponseDTO {

    private Long allocationId;
    private String studentName;
    private String enrollmentNumber;
    private Integer rollNo;
    private String seatLabel;

    /** 0-based position index: 0=LEFT, 1=MIDDLE, 2=RIGHT */
    private Integer positionIndex;
    /** Human-readable position label: "LEFT", "MIDDLE", "RIGHT", or "" for single */
    private String positionLabel;

    private Long seatId;
    private java.util.UUID studentId;
    private String roomName;
    private int rowNumber;
    private int columnNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** Subject name for this allocation's schedule */
    private String subjectName;
    /** Class name for this allocation's schedule */
    private String className;
}
