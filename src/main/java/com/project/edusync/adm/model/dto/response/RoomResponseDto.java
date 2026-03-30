package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

/**
 * DTO for responding with Room information.
 */
@Data
@Builder
public class RoomResponseDto {

    private UUID uuid;
    private String name;
    private String roomType;
    private String seatingType;
    private Integer rowCount;
    private Integer columnsPerRow;
    private Integer seatsPerUnit;
    private Integer totalCapacity;
    private Integer floorNumber;
    private BuildingResponseDto building;
    private Boolean hasProjector;
    private Boolean hasAC;
    private Boolean hasWhiteboard;
    private Boolean isAccessible;
    private String otherAmenities;

}