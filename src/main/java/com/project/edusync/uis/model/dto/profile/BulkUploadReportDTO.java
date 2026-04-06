package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadReportDTO {
    private int success;
    private int failed;
    private List<String> errors;
}
