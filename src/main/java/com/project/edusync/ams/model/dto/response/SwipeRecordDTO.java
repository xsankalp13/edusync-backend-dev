package com.project.edusync.ams.model.dto.response;
import com.project.edusync.em.model.enums.StudentAttendanceStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SwipeRecordDTO {
    private Long studentId;
    private LocalDate date;
    private StudentAttendanceStatus status;
}
