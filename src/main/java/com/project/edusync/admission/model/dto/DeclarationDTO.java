package com.project.edusync.admission.model.dto;
import lombok.Data;
import java.time.LocalDate;
@Data
public class DeclarationDTO {
    private boolean informationCorrect;
    private boolean agreesToRules;
    private String signatureUrl;
    private LocalDate declarationDate;
}
