package com.project.edusync.admission.model.dto;
import lombok.Data;
import java.time.LocalDate;
@Data
public class StudentBasicDetailsDTO {
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String nationality;
    private String religion;
    private String caste;
    private String aadhaarNumber;
    private String motherTongue;
    private String category;
}
