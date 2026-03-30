package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/*
    Dto for sending response for class section entity
    contains only data that can be sent to a client
 */


@Data
@Builder
public class SectionResponseDto {

    private UUID uuid;
    private String sectionName;
    private RoomBasicResponseDto defaultRoom;

}