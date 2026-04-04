package com.project.edusync.em.model.dto.response;

public class GradingScaleResponseDTO {
    private Long id;
    private Integer minMarks;
    private Integer maxMarks;
    private String grade;
    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getMinMarks() { return minMarks; }
    public void setMinMarks(Integer minMarks) { this.minMarks = minMarks; }
    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}

