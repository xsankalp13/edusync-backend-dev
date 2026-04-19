package com.project.edusync.em.model.dto.internal.admitbatch;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdmitCardData {
    StudentDTO student;
    List<ScheduleDTO> schedules;
    String admitCardNumber;
    String qrCodeBase64;
    String verificationCode;
    String examType;
    String issueDate;
}
