package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherDetailsDTO {
    private String certifications;
    private String specializations;
    private Integer yearsOfExperience;
    private String educationLevel;
    private String stateLicenseNumber;
    private List<UUID> teachableSubjectIds;
}
