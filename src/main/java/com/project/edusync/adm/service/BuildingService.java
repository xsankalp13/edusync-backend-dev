package com.project.edusync.adm.service;

import com.project.edusync.adm.model.dto.request.BuildingRequestDto;
import com.project.edusync.adm.model.dto.response.BuildingResponseDto;

import java.util.List;
import java.util.UUID;

public interface BuildingService {

    BuildingResponseDto createBuilding(BuildingRequestDto requestDto);

    List<BuildingResponseDto> getAllBuildings();

    BuildingResponseDto updateBuilding(UUID buildingId, BuildingRequestDto requestDto);

    void deleteBuilding(UUID buildingId);
}

