package com.project.edusync.admission.model.dto;
import lombok.Data;
@Data
public class MedicalInfoDTO {
    private String allergies;
    private String existingMedicalConditions;
    private String disabilities;
    private String emergencyContactPerson;
    private String emergencyContactNumber;
}
