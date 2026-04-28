package com.project.edusync.em.model.dto.request;

import com.project.edusync.em.model.enums.ExamAttendanceStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamAttendanceMarkEntryDTO {

    @NotNull
    private Long studentId;

    @NotNull
    private ExamAttendanceStatus status;

    @JsonAlias({"isMalpractice", "malpracticeReported"})
    private Boolean malpractice;
}

