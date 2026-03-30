package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.request.RoomRequestDto;
import com.project.edusync.adm.model.dto.response.RoomResponseDto;
import com.project.edusync.adm.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Rooms.
 * All responses are wrapped in ResponseEntity for full control over the HTTP response.
 */
@RestController
@RequestMapping("${api.url}/auth") // Following your existing request mapping
@RequiredArgsConstructor
@Tag(name = "Room Management", description = "Endpoints for managing rooms, seating layout, location, and amenities")
public class RoomController {

    private final RoomService roomService;

    /**
     * Creates a new room (e.g., "Room 101").
     * HTTP 201 Created on success.
     */
    @PostMapping("/rooms")
    @Operation(
            summary = "Create room",
            description = "Creates a room with seating layout, floor, building, and amenities metadata.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Room created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Building not found"),
            @ApiResponse(responseCode = "409", description = "Room name already exists")
    })
    public ResponseEntity<RoomResponseDto> createRoom(
            @Valid @RequestBody RoomRequestDto requestDto) {

        RoomResponseDto createdRoom = roomService.addRoom(requestDto);
        return new ResponseEntity<>(createdRoom, HttpStatus.CREATED);
    }

    /**
     * Retrieves a list of all active rooms.
     * HTTP 200 OK on success.
     */
    @GetMapping("/rooms")
    @Operation(
            summary = "List rooms",
            description = "Returns all active rooms with layout, location and amenities.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rooms fetched successfully")
    })
    public ResponseEntity<List<RoomResponseDto>> getAllRooms() {
        List<RoomResponseDto> response = roomService.getAllRooms();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves details for a single room by its UUID.
     * HTTP 200 OK on success.
     */
    @GetMapping("/rooms/{roomId}")
    @Operation(
            summary = "Get room by ID",
            description = "Returns room details by UUID.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<RoomResponseDto> getRoomById(
            @Parameter(description = "Room UUID", required = true)
            @PathVariable UUID roomId) {

        RoomResponseDto response = roomService.getRoomById(roomId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Updates a room's details by its UUID.
     * HTTP 200 OK on success.
     */
    @PutMapping("/rooms/{roomId}")
    @Operation(
            summary = "Update room",
            description = "Updates room details including seating layout, location, and amenities.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Room or building not found"),
            @ApiResponse(responseCode = "409", description = "Room name already exists")
    })
    public ResponseEntity<RoomResponseDto> updateRoomById(
            @Parameter(description = "Room UUID", required = true)
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomRequestDto roomRequestDto) {

        RoomResponseDto response = roomService.updateRoom(roomId, roomRequestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Soft deletes a room by its UUID.
     * HTTP 204 No Content on success.
     */
    @DeleteMapping("/rooms/{roomId}")
    @Operation(
            summary = "Delete room",
            description = "Soft deletes a room by UUID.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Room deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "409", description = "Room is mapped in timetable and cannot be deleted")
    })
    public ResponseEntity<Void> deleteRoomById(@Parameter(description = "Room UUID", required = true) @PathVariable UUID roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}