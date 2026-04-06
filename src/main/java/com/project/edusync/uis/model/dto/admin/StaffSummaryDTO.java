package com.project.edusync.uis.model.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.model.enums.StaffType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A lightweight summary DTO for a Staff member — used in paginated admin list views.
 * Intentionally excludes deep nested objects (certifications, specializations)
 * to keep list responses fast and clean.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StaffSummaryDTO {

    // --- Identity ---
    private Long staffId;
    private String uuid;
    private String employeeId;

    // --- Personal Info (from UserProfile) ---
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String username;
    private String profileUrl;
    private LocalDate dateOfBirth;
    private String gender;

    // --- Professional Info ---
    private String jobTitle;
    private String department;
    private StaffType staffType;
    private StaffCategory category;
    private String designationCode;
    private String designationName;
    private LocalDate hireDate;
    private String officeLocation;
    private boolean active;

    // --- Teaching Competencies (only populated for TEACHER staff type) ---
    private List<UUID> teachableSubjectIds;
}





