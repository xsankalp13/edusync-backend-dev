package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.overtime.OvertimeDTOs.*;
import com.project.edusync.hrms.model.entity.*;
import com.project.edusync.hrms.model.enums.OvertimeStatus;
import com.project.edusync.hrms.repository.*;
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

@Service("overtimeCompOffService")
@RequiredArgsConstructor
public class OvertimeCompOffServiceImpl {

    private final OvertimeRecordRepository overtimeRepo;
    private final CompOffRecordRepository compOffRepo;
    private final LeaveBalanceRepository leaveBalanceRepo;
    private final LeaveTypeConfigRepository leaveTypeRepo;
    private final StaffRepository staffRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private StaffSalaryMappingRepository mappingRepo;

    @org.springframework.beans.factory.annotation.Autowired
    private com.project.edusync.hrms.service.StaffSalaryMappingService mappingService;

    @Transactional
    public OvertimeResponseDTO createOvertime(OvertimeCreateDTO dto) {
        Staff staff = resolveStaff(dto.staffRef());
        OvertimeRecord rec = new OvertimeRecord();
        rec.setStaff(staff);
        rec.setWorkDate(dto.workDate());
        rec.setHoursWorked(dto.hoursWorked());
        rec.setReason(dto.reason());
        rec.setCompensationType(dto.compensationType() != null ? dto.compensationType() : "CASH");
        rec.setStatus(OvertimeStatus.PENDING);
        return toOTResponse(overtimeRepo.save(rec));
    }

    @Transactional(readOnly = true)
    public List<OvertimeResponseDTO> listOvertime(String staffRef, OvertimeStatus status) {
        if (staffRef != null) {
            Staff staff = resolveStaff(staffRef);
            return (status != null ? overtimeRepo.findByStaff_IdAndStatus(staff.getId(), status)
                    : overtimeRepo.findByStaff_Id(staff.getId())).stream().map(this::toOTResponse).toList();
        }
        return (status != null ? overtimeRepo.findByStatus(status) : overtimeRepo.findAll()).stream().map(this::toOTResponse).toList();
    }

    @Transactional
    public OvertimeResponseDTO approveOvertime(UUID uuid, UUID approverRef, OvertimeApproveDTO dto) {
        OvertimeRecord rec = overtimeRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime record not found: " + uuid));
        rec.setStatus(OvertimeStatus.APPROVED);
        rec.setApprovedByRef(approverRef);
        rec.setApprovedAt(LocalDateTime.now());
        
        if ("CASH".equals(rec.getCompensationType())) {
            BigDecimal multiplier = (dto != null && dto.multiplier() != null) ? dto.multiplier() : new BigDecimal("1.50");
            rec.setMultiplier(multiplier);
            
            // Calculate exact approved amount
            java.util.List<StaffSalaryMapping> activeMappings = mappingRepo.findActiveMappingsEffectiveInRange(
                    rec.getWorkDate(), rec.getWorkDate(), java.time.LocalDate.of(9999, 12, 31));
            
            StaffSalaryMapping currentMapping = activeMappings.stream()
                .filter(m -> m.getStaff().getId().equals(rec.getStaff().getId()))
                .findFirst()
                .orElse(null);

            if (currentMapping != null) {
                com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO computed = mappingService.computeBreakdown(currentMapping.getId());
                
                // Assuming basic pay is named "BASIC" or we can just use total earnings * 0.4 etc. 
                // Wait, ComputedSalaryBreakdownDTO has grossPay(). 
                // Let's use the standard formula: (Gross Pay / 30 / 8) * Hours * Multiplier
                // Or if we need exact Basic Pay:
                BigDecimal basicAmount = computed.earnings().stream()
                        .filter(e -> "BASIC".equalsIgnoreCase(e.componentCode()) || "BASIC".equalsIgnoreCase(e.componentName()))
                        .map(com.project.edusync.hrms.dto.salary.ComputedComponentDTO::computedAmount)
                        .findFirst()
                        .orElse(computed.grossPay().multiply(new BigDecimal("0.40"))); // fallback 40% of gross
                        
                // Formula: Hourly Rate = (Basic / 30) / 8
                BigDecimal hourlyRate = basicAmount
                        .divide(new BigDecimal("30"), 4, java.math.RoundingMode.HALF_UP)
                        .divide(new BigDecimal("8"), 4, java.math.RoundingMode.HALF_UP);
                        
                BigDecimal approvedAmt = rec.getHoursWorked()
                        .multiply(hourlyRate)
                        .multiply(multiplier)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                        
                rec.setApprovedAmount(approvedAmt);
            } else {
                rec.setApprovedAmount(BigDecimal.ZERO); // fallback if no active mapping
            }
        }
        
        return toOTResponse(overtimeRepo.save(rec));
    }

    @Transactional
    public OvertimeResponseDTO rejectOvertime(UUID uuid, String remarks) {
        OvertimeRecord rec = overtimeRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime record not found: " + uuid));
        if (rec.getStatus() != OvertimeStatus.PENDING)
            throw new EdusyncException("Can only reject PENDING overtime requests", org.springframework.http.HttpStatus.BAD_REQUEST);
        rec.setStatus(OvertimeStatus.REJECTED);
        rec.setRejectedAt(LocalDateTime.now());
        rec.setRejectionRemarks(remarks);
        return toOTResponse(overtimeRepo.save(rec));
    }

    @Transactional
    public CompOffResponseDTO createCompOff(CompOffCreateDTO dto) {
        Staff staff = resolveStaff(dto.staffRef());
        CompOffRecord rec = new CompOffRecord();
        rec.setStaff(staff);
        rec.setCreditDate(dto.creditDate());
        rec.setExpiryDate(dto.expiryDate());
        rec.setRemarks(dto.remarks());
        if (dto.overtimeRecordRef() != null) {
            overtimeRepo.findByUuid(UUID.fromString(dto.overtimeRecordRef()))
                    .ifPresent(rec::setOvertimeRecord);
        }
        if (dto.leaveTypeRef() != null) {
            leaveTypeRepo.findByUuid(UUID.fromString(dto.leaveTypeRef()))
                    .ifPresent(rec::setLeaveType);
        }
        return toCompOffResponse(compOffRepo.save(rec));
    }

    @Transactional(readOnly = true)
    public List<CompOffResponseDTO> listCompOff(String staffRef) {
        Staff staff = resolveStaff(staffRef);
        return compOffRepo.findByStaff_Id(staff.getId()).stream().map(this::toCompOffResponse).toList();
    }

    @Transactional
    public CompOffResponseDTO creditCompOff(UUID uuid) {
        CompOffRecord rec = compOffRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("CompOff not found: " + uuid));
        if (rec.isCredited())
            throw new EdusyncException("Already credited", HttpStatus.BAD_REQUEST);
        rec.setCredited(true);
        rec.setCreditedAt(LocalDateTime.now());
        compOffRepo.save(rec);

        if (rec.getLeaveType() != null) {
            String currentYear = String.valueOf(java.time.LocalDate.now().getYear());
            leaveBalanceRepo.findByStaff_IdAndLeaveType_IdAndAcademicYearAndActiveTrue(
                    rec.getStaff().getId(), rec.getLeaveType().getId(), currentYear)
                    .ifPresent(lb -> {
                        lb.setTotalQuota(lb.getTotalQuota().add(BigDecimal.ONE));
                        leaveBalanceRepo.save(lb);
                    });
        }
        return toCompOffResponse(rec);
    }

    @Transactional(readOnly = true)
    public CompOffBalanceSummaryDTO getCompOffSummary(String staffRef) {
        Staff staff = resolveStaff(staffRef);
        String name = staff.getUserProfile() != null ? (staff.getUserProfile().getFirstName() + " " + staff.getUserProfile().getLastName()).trim() : "";
        List<CompOffRecord> available = compOffRepo.findByStaff_IdAndCredited(staff.getId(), true)
                .stream().filter(r -> r.getExpiryDate() == null || !r.getExpiryDate().isBefore(java.time.LocalDate.now())).toList();
        List<CompOffBalanceSummaryDTO.AvailableCompOff> avail = available.stream().map(r ->
                new CompOffBalanceSummaryDTO.AvailableCompOff(r.getUuid(), r.getCreditDate(), r.getExpiryDate(),
                        r.getLeaveType() != null ? r.getLeaveType().getDisplayName() : null)).toList();
        return new CompOffBalanceSummaryDTO(staff.getUuid(), name, avail);
    }

    private Staff resolveStaff(String ref) {
        return PublicIdentifierResolver.resolve(ref, staffRepository::findByUuid, staffRepository::findById, "Staff");
    }

    private String name(Staff s) {
        return s.getUserProfile() != null ? (s.getUserProfile().getFirstName() + " " + s.getUserProfile().getLastName()).trim() : "";
    }

    private OvertimeResponseDTO toOTResponse(OvertimeRecord r) {
        return new OvertimeResponseDTO(r.getUuid(), r.getStaff().getUuid(), name(r.getStaff()),
                r.getWorkDate(), r.getHoursWorked(), r.getReason(), r.getStatus(),
                r.getCompensationType(), r.getApprovedAt(),
                r.getMultiplier(), r.getApprovedAmount(), r.getPayrollRunRef());
    }

    private CompOffResponseDTO toCompOffResponse(CompOffRecord r) {
        return new CompOffResponseDTO(r.getUuid(), r.getStaff().getUuid(), name(r.getStaff()),
                r.getOvertimeRecord() != null ? r.getOvertimeRecord().getUuid() : null,
                r.getLeaveType() != null ? r.getLeaveType().getUuid() : null,
                r.getLeaveType() != null ? r.getLeaveType().getDisplayName() : null,
                r.getCreditDate(), r.getExpiryDate(), r.isCredited(), r.getCreditedAt(), r.getRemarks());
    }
}





