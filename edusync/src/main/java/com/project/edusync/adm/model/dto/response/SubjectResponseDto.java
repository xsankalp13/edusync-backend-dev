package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

/**
 * DTO for responding with Subject information.
 */
@Data
@Builder
public class SubjectResponseDto {

    private UUID uuid;
    private String name;
    private String subjectCode;
    private String requiresSpecialRoomType;

}