package com.project.edusync.em.model.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExamAttendanceFinalizeResponseDTO {
    int totalStudents;
    int alreadyMarked;
    int autoMarkedAbsent;
    boolean finalized;
}

