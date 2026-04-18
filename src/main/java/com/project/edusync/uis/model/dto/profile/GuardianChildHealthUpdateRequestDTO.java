package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.project.edusync.uis.model.dto.profile.StudentMedicalRecordDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuardianChildHealthUpdateRequestDTO {
    private String bloodGroup;
    private StudentMedicalRecordDTO medicalRecord;
}


