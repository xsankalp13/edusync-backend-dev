package com.project.edusync.adm.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

/**
 * DTO for creating or updating a Section.
 */
@Data
public class SectionRequestDto {

    @NotBlank(message = "Section name cannot be blank")
    private String sectionName;

    private UUID defaultRoomId;

}