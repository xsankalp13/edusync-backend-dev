package com.project.edusync.em.model.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GlobalSeatAllocationResultDTO {
    private int totalStudents;
    private int totalSeated;
    private int totalUnseated;
    private int roomsUsed;
    private int timeslotsProcessed;
    @Builder.Default
    private List<String> skippedStudents = new ArrayList<>();
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
