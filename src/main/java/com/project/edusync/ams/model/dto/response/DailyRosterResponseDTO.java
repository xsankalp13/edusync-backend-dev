package com.project.edusync.ams.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyRosterResponseDTO {
    private Long studentId;
    private String name;
    private boolean hasApprovedLeave;
}
