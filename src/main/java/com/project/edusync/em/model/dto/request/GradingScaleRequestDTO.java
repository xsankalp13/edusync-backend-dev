package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class GradingScaleRequestDTO {
    @NotNull
    private Integer minMarks;
    @NotNull
    private Integer maxMarks;
    @NotNull
    @Size(min = 1, max = 10)
    private String grade;
    // getters and setters
    public Integer getMinMarks() { return minMarks; }
    public void setMinMarks(Integer minMarks) { this.minMarks = minMarks; }
    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}

