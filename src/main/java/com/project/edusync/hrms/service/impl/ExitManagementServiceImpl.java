package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.exit.ExitDTOs;
import com.project.edusync.hrms.dto.exit.ExitDTOs.*;
import com.project.edusync.hrms.model.entity.*;
import com.project.edusync.hrms.model.enums.ExitRequestStatus;
import com.project.edusync.hrms.model.enums.FnFStatus;
import com.project.edusync.hrms.repository.*;
import com.project.edusync.hrms.service.ExitManagementService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExitManagementServiceImpl implements ExitManagementService {

    private final ExitRequestRepository exitRequestRepo;
    private final ExitClearanceItemRepository clearanceItemRepo;
    private final FullFinalSettlementRepository fnfRepo;
    private final StaffRepository staffRepository;

    @Override @Transactional
    public ExitRequestResponseDTO createExitRequest(ExitRequestCreateDTO dto) {
        Staff staff = resolveStaff(dto.staffRef());
        ExitRequest req = new ExitRequest();
        req.setStaff(staff);
        req.setResignationDate(dto.resignationDate());
        req.setLastWorkingDate(dto.lastWorkingDate());
        req.setExitReason(dto.exitReason());
        req.setStatus(ExitRequestStatus.SUBMITTED);
        return toResponse(exitRequestRepo.save(req));
    }

    @Override @Transactional
    public ExitRequestResponseDTO updateStatus(UUID uuid, ExitStatusUpdateDTO dto) {
        ExitRequest req = findRequest(uuid);
        req.setStatus(dto.status());
        if (dto.status() == ExitRequestStatus.COMPLETED || dto.status() == ExitRequestStatus.WITHDRAWN) {
            Staff staff = req.getStaff();
            if (dto.status() == ExitRequestStatus.COMPLETED) {
                staff.setActive(false);
                staff.setTerminationDate(java.time.LocalDate.now());
                staffRepository.save(staff);
            }
        }
        return toResponse(exitRequestRepo.save(req));
    }

    @Override @Transactional(readOnly = true)
    public List<ExitRequestResponseDTO> listExitRequests(ExitRequestStatus status) {
        List<ExitRequest> list = status != null ? exitRequestRepo.findByStatus(status) : exitRequestRepo.findAllByActiveTrue();
        return list.stream().map(this::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public ExitRequestResponseDTO getExitRequest(UUID uuid) {
        return toResponse(findRequest(uuid));
    }

    @Override @Transactional
    public ClearanceItemResponseDTO addClearanceItem(UUID exitRequestUuid, ClearanceItemCreateDTO dto) {
        ExitRequest req = findRequest(exitRequestUuid);
        ExitClearanceItem item = new ExitClearanceItem();
        item.setExitRequest(req);
        item.setItemType(dto.itemType());
        item.setDescription(dto.description());
        item.setResponsiblePartyRef(dto.responsiblePartyRef());
        item.setRemarks(dto.remarks());
        return toItemResponse(clearanceItemRepo.save(item));
    }

    @Override @Transactional(readOnly = true)
    public List<ClearanceItemResponseDTO> listClearanceItems(UUID exitRequestUuid) {
        ExitRequest req = findRequest(exitRequestUuid);
        return clearanceItemRepo.findByExitRequest_IdAndActiveTrue(req.getId())
                .stream().map(this::toItemResponse).toList();
    }

    @Override @Transactional
    public ClearanceItemResponseDTO completeClearanceItem(UUID exitRequestUuid, Long itemId, String completedByName, String remarks) {
        ExitRequest req = findRequest(exitRequestUuid);
        ExitClearanceItem item = clearanceItemRepo.findByIdAndExitRequest_Id(itemId, req.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Clearance item not found: " + itemId));
        item.setCompletedAt(LocalDateTime.now());
        item.setCompletedByName(completedByName);
        item.setRemarks(remarks);
        return toItemResponse(clearanceItemRepo.save(item));
    }

    @Override @Transactional
    public ClearanceItemResponseDTO waivedClearanceItem(UUID exitRequestUuid, Long itemId, ExitDTOs.WaiveClearanceItemDTO dto) {
        ExitRequest req = findRequest(exitRequestUuid);
        ExitClearanceItem item = clearanceItemRepo.findByIdAndExitRequest_Id(itemId, req.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Clearance item not found: " + itemId));
        if (item.getCompletedAt() != null)
            throw new EdusyncException("Clearance item is already completed", org.springframework.http.HttpStatus.BAD_REQUEST);
        item.setWaived(true);
        item.setWaivedBy(dto.waivedBy());
        item.setWaivedAt(LocalDateTime.now());
        if (dto.remarks() != null) item.setRemarks(dto.remarks());
        return toItemResponse(clearanceItemRepo.save(item));
    }

    @Override @Transactional
    public FnFResponseDTO createFnF(UUID exitRequestUuid, FnFCreateDTO dto) {
        ExitRequest req = findRequest(exitRequestUuid);
        if (fnfRepo.findByExitRequest_Id(req.getId()).isPresent())
            throw new EdusyncException("FnF already exists for this exit request", HttpStatus.CONFLICT);
        FullFinalSettlement fnf = new FullFinalSettlement();
        fnf.setExitRequest(req);
        applyFnFCreate(fnf, dto);
        recalcNet(fnf);
        return toFnFResponse(fnfRepo.save(fnf));
    }

    @Override @Transactional(readOnly = true)
    public FnFResponseDTO getFnF(UUID exitRequestUuid) {
        ExitRequest req = findRequest(exitRequestUuid);
        return toFnFResponse(fnfRepo.findByExitRequest_Id(req.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FnF not found for exit request: " + exitRequestUuid)));
    }

    @Override @Transactional
    public FnFResponseDTO updateFnFStatus(UUID exitRequestUuid, FnFStatusUpdateDTO dto) {
        ExitRequest req = findRequest(exitRequestUuid);
        FullFinalSettlement fnf = fnfRepo.findByExitRequest_Id(req.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FnF not found"));
        fnf.setStatus(dto.status());
        if (dto.status() == FnFStatus.DISBURSED) fnf.setDisbursedAt(LocalDateTime.now());
        fnf.setRemarks(dto.remarks());
        return toFnFResponse(fnfRepo.save(fnf));
    }

    private ExitRequest findRequest(UUID uuid) {
        return exitRequestRepo.findByUuidAndActiveTrue(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Exit request not found: " + uuid));
    }

    private Staff resolveStaff(String ref) {
        return PublicIdentifierResolver.resolve(ref, staffRepository::findByUuid, staffRepository::findById, "Staff");
    }

    private String staffName(Staff s) {
        return s.getUserProfile() != null
                ? (s.getUserProfile().getFirstName() + " " + s.getUserProfile().getLastName()).trim() : "";
    }

    private ExitRequestResponseDTO toResponse(ExitRequest r) {
        return new ExitRequestResponseDTO(r.getUuid(), r.getStaff().getUuid(), staffName(r.getStaff()),
                r.getResignationDate(), r.getLastWorkingDate(), r.getExitReason(), r.getStatus(), r.getCreatedAt());
    }

    private ClearanceItemResponseDTO toItemResponse(ExitClearanceItem i) {
        return new ClearanceItemResponseDTO(i.getId(), i.getUuid(), i.getItemType(), i.getDescription(),
                i.getCompletedAt(), i.getCompletedByName(),
                i.isWaived(), i.getWaivedBy(), i.getWaivedAt(),
                i.getRemarks());
    }

    private void applyFnFCreate(FullFinalSettlement fnf, FnFCreateDTO dto) {
        fnf.setGrossSalaryDue(nvl(dto.grossSalaryDue()));
        fnf.setDeductions(nvl(dto.deductions()));
        fnf.setLeaveEncashment(nvl(dto.leaveEncashment()));
        fnf.setGratuity(nvl(dto.gratuity()));
        fnf.setOtherAdditions(nvl(dto.otherAdditions()));
        fnf.setOtherDeductions(nvl(dto.otherDeductions()));
        if (dto.remarks() != null) fnf.setRemarks(dto.remarks());
    }

    private void recalcNet(FullFinalSettlement fnf) {
        BigDecimal net = fnf.getGrossSalaryDue()
                .subtract(fnf.getDeductions())
                .add(fnf.getLeaveEncashment())
                .add(fnf.getGratuity())
                .add(fnf.getOtherAdditions())
                .subtract(fnf.getOtherDeductions());
        fnf.setNetPayable(net);
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private FnFResponseDTO toFnFResponse(FullFinalSettlement f) {
        return new FnFResponseDTO(f.getUuid(), f.getExitRequest().getUuid(),
                f.getGrossSalaryDue(), f.getDeductions(), f.getLeaveEncashment(),
                f.getGratuity(), f.getOtherAdditions(), f.getOtherDeductions(),
                f.getNetPayable(), f.getStatus(), f.getDisbursedAt(), f.getRemarks());
    }
}

