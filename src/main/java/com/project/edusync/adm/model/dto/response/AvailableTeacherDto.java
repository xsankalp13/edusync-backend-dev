package com.project.edusync.adm.model.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for responding with available Teacher (ID and Name).
 * Uses Long for id.
 */
@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class AvailableTeacherDto {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
}