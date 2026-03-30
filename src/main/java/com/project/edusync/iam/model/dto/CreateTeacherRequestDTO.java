package com.project.edusync.iam.model.dto;

import com.project.edusync.uis.model.enums.StaffType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateTeacherRequestDTO extends BaseStaffRequestDTO {

    private String stateLicenseNumber;
    private String educationLevel; // Enum as String
    private Integer yearsOfExperience;

    // Arrays for the JSON columns
    private List<String> specializations;
    private List<String> certifications;
    private List<String> classesToTeach;
    private List<UUID> teachableSubjectIds;

    @Override
    public StaffType getStaffType() {
        return StaffType.TEACHER;
    }
}