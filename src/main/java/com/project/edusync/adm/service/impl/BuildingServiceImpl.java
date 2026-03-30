package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.exception.DuplicateEntryException;
import com.project.edusync.adm.exception.InvalidRequestException;
import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.model.dto.request.BuildingRequestDto;
import com.project.edusync.adm.model.dto.response.BuildingResponseDto;
import com.project.edusync.adm.model.entity.Building;
import com.project.edusync.adm.repository.BuildingRepository;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.adm.service.BuildingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;

    @Override
    public BuildingResponseDto createBuilding(BuildingRequestDto requestDto) {
        String buildingName = requestDto.name().trim();
        if (buildingRepository.existsByNameIgnoreCase(buildingName)) {
            throw new DuplicateEntryException("Building with name '" + buildingName + "' already exists.");
        }

        Building building = new Building();
        building.setName(buildingName);
        building.setCode(requestDto.code());
        building.setTotalFloors(requestDto.totalFloors());

        return toDto(buildingRepository.save(building));
    }

    @Override
    public List<BuildingResponseDto> getAllBuildings() {
        return buildingRepository.findAll().stream()
                .sorted(Comparator.comparing(Building::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BuildingResponseDto updateBuilding(UUID buildingId, BuildingRequestDto requestDto) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));

        String newName = requestDto.name().trim();
        if (buildingRepository.existsByNameIgnoreCaseAndUuidNot(newName, buildingId)) {
            throw new DuplicateEntryException("Building with name '" + newName + "' already exists.");
        }

        building.setName(newName);
        building.setCode(requestDto.code());
        building.setTotalFloors(requestDto.totalFloors());

        return toDto(buildingRepository.save(building));
    }

    @Override
    @Transactional
    public void deleteBuilding(UUID buildingId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));

        if (roomRepository.existsByBuildingUuid(buildingId)) {
            throw new InvalidRequestException("Cannot delete building '" + building.getName() + "' because rooms reference it.");
        }

        buildingRepository.delete(building);
        log.info("Building '{}' deleted successfully", building.getName());
    }

    private BuildingResponseDto toDto(Building building) {
        return new BuildingResponseDto(
                building.getUuid(),
                building.getName(),
                building.getCode(),
                building.getTotalFloors()
        );
    }
}

