package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherDetailsDTO {
    private String certifications;
    private String specializations;
    private Integer yearsOfExperience;
    private String educationLevel;
    private String stateLicenseNumber;
    private List<TeacherSubjectDTO> teachableSubjects;
}
