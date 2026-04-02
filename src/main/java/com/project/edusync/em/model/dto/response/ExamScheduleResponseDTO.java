package com.project.edusync.em.model.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExamScheduleResponseDTO {
    private Long id;
    private LocalDate examDate;
    private Integer duration;
    private Integer maxMarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long academicClassId;
    private String academicClassName;
    private Long timeslotId;
    private String timeslotLabel;
    private Long subjectId;
    private String subjectName;
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;
    private java.math.BigDecimal passingMarks;
    private Integer maxStudentsPerSeat;
    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getAcademicClassId() { return academicClassId; }
    public void setAcademicClassId(Long academicClassId) { this.academicClassId = academicClassId; }
    public String getAcademicClassName() { return academicClassName; }
    public void setAcademicClassName(String academicClassName) { this.academicClassName = academicClassName; }
    public Long getTimeslotId() { return timeslotId; }
    public void setTimeslotId(Long timeslotId) { this.timeslotId = timeslotId; }
    public String getTimeslotLabel() { return timeslotLabel; }
    public void setTimeslotLabel(String timeslotLabel) { this.timeslotLabel = timeslotLabel; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public java.time.LocalTime getStartTime() { return startTime; }
    public void setStartTime(java.time.LocalTime startTime) { this.startTime = startTime; }
    public java.time.LocalTime getEndTime() { return endTime; }
    public void setEndTime(java.time.LocalTime endTime) { this.endTime = endTime; }
    public java.math.BigDecimal getPassingMarks() { return passingMarks; }
    public void setPassingMarks(java.math.BigDecimal passingMarks) { this.passingMarks = passingMarks; }
    public Integer getMaxStudentsPerSeat() { return maxStudentsPerSeat; }
    public void setMaxStudentsPerSeat(Integer maxStudentsPerSeat) { this.maxStudentsPerSeat = maxStudentsPerSeat; }
}
