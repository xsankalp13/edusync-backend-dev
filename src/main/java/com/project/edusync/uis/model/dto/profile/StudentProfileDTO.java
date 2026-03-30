package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentProfileDTO {
    private Long studentId;
    private String enrollmentNo;
    private String enrollmentStatus;
    private String profileUrl;
    private java.time.LocalDate admissionDate;
    private Integer expectedGraduationYear;
    private String counselorName;

    // Nested medical profile
    private StudentMedicalRecordDTO medicalRecord;
}
