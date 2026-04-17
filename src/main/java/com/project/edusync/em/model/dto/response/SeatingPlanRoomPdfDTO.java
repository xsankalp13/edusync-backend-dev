package com.project.edusync.em.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatingPlanRoomPdfDTO {
    private String roomName;
    private int totalStudents;
    private int maxBenchCount;
    private List<Integer> benchHeaders;
    private List<RowDTO> rows;
}

