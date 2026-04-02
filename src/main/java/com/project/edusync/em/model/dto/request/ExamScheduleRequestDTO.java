package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class ExamScheduleRequestDTO {
    @NotNull
    private Long academicClassId;
    @NotNull
    private Long timeslotId;
    @NotNull
    private Long subjectId;
    @NotNull
    private LocalDate examDate;
    @NotNull
    @Positive
    private Integer duration;
    @NotNull
    @Positive
    private Integer maxMarks;
    @Positive
    private Integer passingMarks;
    private Integer maxStudentsPerSeat;
    // getters and setters
    public Long getAcademicClassId() { return academicClassId; }
    public void setAcademicClassId(Long academicClassId) { this.academicClassId = academicClassId; }
    public Long getTimeslotId() { return timeslotId; }
    public void setTimeslotId(Long timeslotId) { this.timeslotId = timeslotId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }
    public Integer getPassingMarks() { return passingMarks; }
    public void setPassingMarks(Integer passingMarks) { this.passingMarks = passingMarks; }
    public Integer getMaxStudentsPerSeat() { return maxStudentsPerSeat; }
    public void setMaxStudentsPerSeat(Integer maxStudentsPerSeat) { this.maxStudentsPerSeat = maxStudentsPerSeat; }
}

