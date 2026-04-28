package com.project.edusync.em.model.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExamAttendanceMarkResponseDTO {
    int savedCount;
    boolean finalized;
}

