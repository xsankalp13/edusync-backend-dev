package com.project.edusync.admission.model.dto;
import lombok.Data;
@Data
public class AdmissionDetailsDTO {
    private String classApplyingFor;
    private String academicYear;
    private String stream;
    private String secondLanguagePreference;
    private String thirdLanguagePreference;
    private boolean transportRequired;
    private boolean hostelRequired;
}
