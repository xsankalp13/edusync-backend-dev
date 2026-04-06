package com.project.edusync.teacher.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TeacherStudentResponseDto {
    private UUID uuid;
    private String firstName;
    private String lastName;
    private String profileUrl;
    private String enrollmentNo;
    private String rollNumber;
    private String className;
    private String sectionName;
    private UUID classUuid;
    private UUID sectionUuid;
    private String guardianName;
    private String guardianPhone;
    private BigDecimal attendancePercentage;
    private long totalPresent;
    private long totalAbsent;
    private long totalWorkingDays;
}

