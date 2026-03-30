package com.project.edusync.adm.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * DTO for creating or updating a Room.
 */
@Data
public class RoomRequestDto {

    @NotBlank(message = "Room name cannot be blank")
    @Size(max = 100)
    private String name;

    @NotBlank
    @Pattern(regexp = "CLASSROOM|LABORATORY|COMPUTER_LAB|LIBRARY|OTHER", message = "Invalid roomType")
    private String roomType;

    @NotBlank
    @Pattern(regexp = "BENCH|DESK_CHAIR|WORKSTATION|TERMINAL", message = "Invalid seatingType")
    private String seatingType;

    @NotNull
    @Min(1)
    private Integer rowCount;

    @NotNull
    @Min(1)
    private Integer columnsPerRow;

    @NotNull
    @Min(1)
    private Integer seatsPerUnit;

    @NotNull
    @Min(0)
    private Integer floorNumber;

    @NotNull
    private UUID buildingId;

    private Boolean hasProjector;
    private Boolean hasAC;
    private Boolean hasWhiteboard;
    private Boolean isAccessible;

    @Size(max = 500)
    private String otherAmenities;
}