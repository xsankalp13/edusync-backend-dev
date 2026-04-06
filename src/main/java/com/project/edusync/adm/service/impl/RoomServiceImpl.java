package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.exception.AlreadyBookedException;
import com.project.edusync.adm.exception.DuplicateEntryException;
import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.model.dto.request.RoomRequestDto;
import com.project.edusync.adm.model.dto.response.BuildingResponseDto;
import com.project.edusync.adm.model.dto.response.RoomResponseDto;
import com.project.edusync.adm.model.entity.Building;
import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.repository.BuildingRepository;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.adm.service.RoomService;
import com.project.edusync.em.model.service.SeatAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeatAllocationService seatAllocationService;

    @Override
    public RoomResponseDto addRoom(RoomRequestDto roomRequestDto) {
        log.info("Attempting to create a new room with name: {}", roomRequestDto.getName());

        // Best Practice: Validate for uniqueness
        if (roomRepository.existsByNameIgnoreCase(roomRequestDto.getName())) {
            log.warn("Room creation failed. Name '{}' already exists.", roomRequestDto.getName());
            throw new DuplicateEntryException("Room with name " + roomRequestDto.getName() + " already exists.");
        }

        Building building = getBuildingOrThrow(roomRequestDto.getBuildingId());

        Room newRoom = new Room();
        newRoom.setName(roomRequestDto.getName());
        newRoom.setRoomType(roomRequestDto.getRoomType());
        newRoom.setSeatingType(roomRequestDto.getSeatingType());
        newRoom.setRowCount(roomRequestDto.getRowCount());
        newRoom.setColumnsPerRow(roomRequestDto.getColumnsPerRow());
        newRoom.setSeatsPerUnit(roomRequestDto.getSeatsPerUnit());
        newRoom.setFloorNumber(roomRequestDto.getFloorNumber());
        newRoom.setBuilding(building);
        newRoom.setHasProjector(Boolean.TRUE.equals(roomRequestDto.getHasProjector()));
        newRoom.setHasAC(Boolean.TRUE.equals(roomRequestDto.getHasAC()));
        newRoom.setHasWhiteboard(roomRequestDto.getHasWhiteboard() == null ? true : roomRequestDto.getHasWhiteboard());
        newRoom.setIsAccessible(Boolean.TRUE.equals(roomRequestDto.getIsAccessible()));
        newRoom.setOtherAmenities(roomRequestDto.getOtherAmenities());
        newRoom.setIsActive(true); // Explicitly set as active

        Room savedRoom = roomRepository.save(newRoom);
        log.info("Room '{}' created successfully with id {}", savedRoom.getName(), savedRoom.getUuid());

        // Auto-generate physical seats for the room
        seatAllocationService.generateSeatsForRoom(savedRoom);

        return toRoomResponseDto(savedRoom);
    }

    @Override
    public List<RoomResponseDto> getAllRooms() {
        log.info("Fetching all active rooms");
        return roomRepository.findAll().stream()
                .filter(Room::getIsActive) // Only return active rooms
                .map(this::toRoomResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoomResponseDto getRoomById(UUID roomId) {
        log.info("Fetching room with id: {}", roomId);
        Room room = roomRepository.findActiveById(roomId)
                .orElseThrow(() -> {
                    log.warn("No active room with id {} found", roomId);
                    return new ResourceNotFoundException("No resource found with id: " + roomId);
                });
        return toRoomResponseDto(room);
    }

    @Override
    @Transactional
    public RoomResponseDto updateRoom(UUID roomId, RoomRequestDto roomRequestDto) {
        log.info("Attempting to update room with id: {}", roomId);
        Room existingRoom = roomRepository.findActiveById(roomId)
                .orElseThrow(() -> {
                    log.warn("No active room with id {} to update", roomId);
                    return new ResourceNotFoundException("No resource found to update with id: " + roomId);
                });

        // Check uniqueness of name only if it's being changed
        if (!existingRoom.getName().equals(roomRequestDto.getName())) {
            if (roomRepository.existsByNameIgnoreCaseAndUuidNot(roomRequestDto.getName(), roomId)) {
                log.warn("Room update failed. Name '{}' already exists for another room.", roomRequestDto.getName());
                throw new DuplicateEntryException("Room with name " + roomRequestDto.getName() + " already exists.");
            }
        }

        Building building = getBuildingOrThrow(roomRequestDto.getBuildingId());

        existingRoom.setName(roomRequestDto.getName());
        existingRoom.setRoomType(roomRequestDto.getRoomType());
        existingRoom.setSeatingType(roomRequestDto.getSeatingType());
        existingRoom.setRowCount(roomRequestDto.getRowCount());
        existingRoom.setColumnsPerRow(roomRequestDto.getColumnsPerRow());
        existingRoom.setSeatsPerUnit(roomRequestDto.getSeatsPerUnit());
        existingRoom.setFloorNumber(roomRequestDto.getFloorNumber());
        existingRoom.setBuilding(building);
        existingRoom.setHasProjector(Boolean.TRUE.equals(roomRequestDto.getHasProjector()));
        existingRoom.setHasAC(Boolean.TRUE.equals(roomRequestDto.getHasAC()));
        existingRoom.setHasWhiteboard(roomRequestDto.getHasWhiteboard() == null ? true : roomRequestDto.getHasWhiteboard());
        existingRoom.setIsAccessible(Boolean.TRUE.equals(roomRequestDto.getIsAccessible()));
        existingRoom.setOtherAmenities(roomRequestDto.getOtherAmenities());

        Room updatedRoom = roomRepository.save(existingRoom);
        log.info("Room with id {} updated successfully", updatedRoom.getUuid());

        // Regenerate physical seats if room capacity/layout changed
        seatAllocationService.generateSeatsForRoom(updatedRoom);

        return toRoomResponseDto(updatedRoom);
    }

    @Override
    @Transactional
    public void deleteRoom(UUID roomId) {
        log.info("Attempting to soft delete room with id: {}", roomId);
        if (!roomRepository.existsActiveById(roomId)) {
            log.warn("Failed to delete. Room not found with id: {}", roomId);
            throw new ResourceNotFoundException("Room id: " + roomId + " not found.");
        }

        if (scheduleRepository.existsActiveByRoomUuid(roomId)) {
            log.warn("Failed to delete. Room id {} is linked to active timetable entries", roomId);
            throw new AlreadyBookedException("Room is currently mapped in timetable. Reassign schedules before deleting this room.");
        }

        roomRepository.softDeleteById(roomId);
        log.info("Room with id {} marked as inactive successfully", roomId);
    }

    /**
     * Private helper to convert Room Entity to Response DTO using builder.
     */
    private RoomResponseDto toRoomResponseDto(Room entity) {
        if (entity == null) return null;
        return RoomResponseDto.builder()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .roomType(entity.getRoomType())
                .seatingType(entity.getSeatingType())
                .rowCount(entity.getRowCount())
                .columnsPerRow(entity.getColumnsPerRow())
                .seatsPerUnit(entity.getSeatsPerUnit())
                .totalCapacity(entity.getTotalCapacity())
                .floorNumber(entity.getFloorNumber())
                .building(entity.getBuilding() == null ? null : new BuildingResponseDto(
                        entity.getBuilding().getUuid(),
                        entity.getBuilding().getName(),
                        entity.getBuilding().getCode(),
                        entity.getBuilding().getTotalFloors()
                ))
                .hasProjector(entity.getHasProjector())
                .hasAC(entity.getHasAC())
                .hasWhiteboard(entity.getHasWhiteboard())
                .isAccessible(entity.getIsAccessible())
                .otherAmenities(entity.getOtherAmenities())
                .build();
    }

    private Building getBuildingOrThrow(UUID buildingId) {
        return buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));
    }
}