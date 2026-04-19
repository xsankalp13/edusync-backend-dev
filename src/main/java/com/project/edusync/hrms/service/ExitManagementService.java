package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.exit.ExitDTOs.*;
import com.project.edusync.hrms.model.enums.ExitRequestStatus;

import java.util.List;
import java.util.UUID;

public interface ExitManagementService {
    ExitRequestResponseDTO createExitRequest(ExitRequestCreateDTO dto);
    ExitRequestResponseDTO updateStatus(UUID uuid, ExitStatusUpdateDTO dto);
    List<ExitRequestResponseDTO> listExitRequests(ExitRequestStatus status);
    ExitRequestResponseDTO getExitRequest(UUID uuid);

    ClearanceItemResponseDTO addClearanceItem(UUID exitRequestUuid, ClearanceItemCreateDTO dto);
    List<ClearanceItemResponseDTO> listClearanceItems(UUID exitRequestUuid);
    ClearanceItemResponseDTO completeClearanceItem(UUID exitRequestUuid, Long itemId, String completedByName, String remarks);
    ClearanceItemResponseDTO waivedClearanceItem(UUID exitRequestUuid, Long itemId, WaiveClearanceItemDTO dto);

    FnFResponseDTO createFnF(UUID exitRequestUuid, FnFCreateDTO dto);
    FnFResponseDTO getFnF(UUID exitRequestUuid);
    FnFResponseDTO updateFnFStatus(UUID exitRequestUuid, FnFStatusUpdateDTO dto);
}

