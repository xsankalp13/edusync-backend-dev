package com.project.edusync.adm.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VisitorLogDto {
    private String name;
    private String gender;
    private String aadhaarNo;
    private String phoneNo;
    private String purpose;
    private String whomToMeet;
    
    // Response fields
    private UUID id;
    private LocalDateTime visitTime;
}
