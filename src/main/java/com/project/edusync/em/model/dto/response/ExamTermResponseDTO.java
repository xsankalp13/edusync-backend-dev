package com.project.edusync.em.model.dto.response;

import java.time.LocalDate;

public class ExamTermResponseDTO {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}

