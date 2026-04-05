package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.payroll.PayrollRunCreateDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunEntryResponseDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunResponseDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunSummaryDTO;
import com.project.edusync.hrms.dto.payroll.PayslipDetailDTO;
import com.project.edusync.hrms.dto.payroll.PayslipLineItemDTO;
import com.project.edusync.hrms.dto.payroll.PayslipSummaryDTO;
import com.project.edusync.hrms.dto.salary.ComputedComponentDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.model.entity.Payslip;
import com.project.edusync.hrms.model.entity.PayslipLineItem;
import com.project.edusync.hrms.model.entity.AcademicCalendarEvent;
import com.project.edusync.hrms.model.entity.PayrollEntry;
import com.project.edusync.hrms.model.entity.PayrollRun;
import com.project.edusync.hrms.model.entity.StaffSalaryMapping;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.model.enums.SalaryComponentType;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.repository.PayslipLineItemRepository;
import com.project.edusync.hrms.repository.PayslipRepository;
import com.project.edusync.hrms.repository.PayrollEntryRepository;
import com.project.edusync.hrms.repository.PayrollRunRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
import com.project.edusync.hrms.service.PayrollService;
import com.project.edusync.hrms.service.StaffSalaryMappingService;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private static final String POLICY_TREAT_UNMARKED_AS_ABSENT = "TREAT_UNMARKED_AS_ABSENT";
    private static final String POLICY_MARKED_ONLY = "MARKED_ONLY";
    private static final String POLICY_FAIL_ON_PARTIAL = "FAIL_ON_PARTIAL";

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollEntryRepository payrollEntryRepository;
    private final PayslipRepository payslipRepository;
    private final PayslipLineItemRepository payslipLineItemRepository;
    private final AcademicCalendarEventRepository academicCalendarEventRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final StaffSalaryMappingRepository staffSalaryMappingRepository;
    private final StaffSalaryMappingService staffSalaryMappingService;
    private final PdfGenerationService pdfGenerationService;
    private final AuthUtil authUtil;
    private final StaffRepository staffRepository;
    private final StaffDailyAttendanceRepository staffDailyAttendanceRepository;

    @Value("${app.hrms.payroll.attendance.partial-mark-policy:TREAT_UNMARKED_AS_ABSENT}")
    private String partialMarkPolicy;

    @Override
    @Transactional
    public PayrollRunResponseDTO createRun(PayrollRunCreateDTO dto) {
        if (payrollRunRepository.existsByPayYearAndPayMonthAndActiveTrue(dto.payYear(), dto.payMonth())) {
            throw new EdusyncException("Payroll run already exists for the selected month", HttpStatus.CONFLICT);
        }

        LocalDate rangeStart = LocalDate.of(dto.payYear(), dto.payMonth(), 1);
        LocalDate rangeEnd = rangeStart.withDayOfMonth(rangeStart.lengthOfMonth());

        List<StaffSalaryMapping> effectiveMappings = staffSalaryMappingRepository.findActiveMappingsEffectiveInRange(
                rangeStart,
                rangeEnd,
                LocalDate.of(9999, 12, 31)
        );

        if (effectiveMappings.isEmpty()) {
            throw new EdusyncException("No active salary mappings found for the selected month", HttpStatus.BAD_REQUEST);
        }

        Map<Long, StaffSalaryMapping> latestMappingByStaff = new LinkedHashMap<>();
        for (StaffSalaryMapping mapping : effectiveMappings) {
            latestMappingByStaff.putIfAbsent(mapping.getStaff().getId(), mapping);
        }

        PayrollRun run = new PayrollRun();
        run.setPayYear(dto.payYear());
        run.setPayMonth(dto.payMonth());
        run.setStatus(PayrollRunStatus.PROCESSED);
        run.setProcessedOn(LocalDateTime.now());
        run.setRemarks(dto.remarks());
        run.setTotalStaff(0);

        PayrollRun savedRun = payrollRunRepository.save(run);

        List<PayrollEntry> savedEntries = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        String academicYear = academicYearForDate(rangeStart);
        int totalWorkingDays = countWorkingDays(rangeStart, rangeEnd, academicYear);

        for (StaffSalaryMapping mapping : latestMappingByStaff.values()) {
            ComputedSalaryBreakdownDTO computed = staffSalaryMappingService.computeBreakdown(mapping.getId());
            BigDecimal lopDays = leaveApplicationRepository.sumApprovedLopDaysOverlapping(
                    mapping.getStaff().getId(),
                    rangeStart,
                    rangeEnd
            );
            if (lopDays == null) {
                lopDays = BigDecimal.ZERO;
            }

            BigDecimal lopDeduction = BigDecimal.ZERO;
            if (totalWorkingDays > 0 && lopDays.compareTo(BigDecimal.ZERO) > 0) {
                lopDeduction = computed.grossPay()
                        .divide(BigDecimal.valueOf(totalWorkingDays), 4, RoundingMode.HALF_UP)
                        .multiply(lopDays)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal adjustedDeductions = computed.totalDeductions().add(lopDeduction).setScale(2, RoundingMode.HALF_UP);
            BigDecimal adjustedNet = computed.grossPay().subtract(adjustedDeductions).setScale(2, RoundingMode.HALF_UP);

            AttendanceMetrics attendanceMetrics = resolveAttendanceMetrics(
                    mapping.getStaff().getId(),
                    rangeStart,
                    rangeEnd,
                    totalWorkingDays,
                    lopDays
            );
            int presentDays = attendanceMetrics.presentDays();
            int absentDays = attendanceMetrics.absentDays();

            PayrollEntry entry = new PayrollEntry();
            entry.setPayrollRun(savedRun);
            entry.setStaff(mapping.getStaff());
            entry.setMapping(mapping);
            entry.setGrossPay(computed.grossPay());
            entry.setTotalDeductions(adjustedDeductions);
            entry.setNetPay(adjustedNet);
            entry.setRemarks(mapping.getRemarks());

            PayrollEntry savedEntry = payrollEntryRepository.save(entry);
            savedEntries.add(savedEntry);

            Payslip payslip = buildPayslip(
                    savedRun,
                    mapping,
                    computed,
                    totalWorkingDays,
                    presentDays,
                    absentDays,
                    lopDays,
                    adjustedDeductions,
                    adjustedNet
            );
            Payslip savedPayslip = payslipRepository.save(payslip);
            saveLineItems(savedPayslip, computed.earnings(), SalaryComponentType.EARNING);
            saveLineItems(savedPayslip, computed.deductions(), SalaryComponentType.DEDUCTION);

            totalGross = totalGross.add(computed.grossPay());
            totalDeductions = totalDeductions.add(adjustedDeductions);
            totalNet = totalNet.add(adjustedNet);
        }

        savedRun.setTotalStaff(savedEntries.size());
        savedRun.setTotalGross(totalGross);
        savedRun.setTotalDeductions(totalDeductions);
        savedRun.setTotalNet(totalNet);

        return toRunResponse(payrollRunRepository.save(savedRun), savedEntries);
    }

    @Override
    @Transactional
    public PayrollRunResponseDTO approveRun(Long runId) {
        PayrollRun run = findActiveRun(runId);
        if (run.getStatus() != PayrollRunStatus.PROCESSED) {
            throw new EdusyncException("Only PROCESSED payroll runs can be approved", HttpStatus.BAD_REQUEST);
        }

        run.setStatus(PayrollRunStatus.APPROVED);
        PayrollRun saved = payrollRunRepository.save(run);
        updatePayslipStatusByRun(runId, PayrollRunStatus.APPROVED);
        return toRunResponse(saved, payrollEntryRepository.findByPayrollRun_IdAndActiveTrueOrderByStaff_IdAsc(runId));
    }

    @Override
    @Transactional
    public PayrollRunResponseDTO approveRunByIdentifier(String identifier) {
        PayrollRun run = findActiveRunByIdentifier(identifier);
        return approveRun(run.getId());
    }

    @Override
    @Transactional
    public PayrollRunResponseDTO disburseRun(Long runId) {
        PayrollRun run = findActiveRun(runId);
        if (run.getStatus() != PayrollRunStatus.APPROVED) {
            throw new EdusyncException("Only APPROVED payroll runs can be marked as disbursed", HttpStatus.BAD_REQUEST);
        }

        run.setStatus(PayrollRunStatus.DISBURSED);
        PayrollRun saved = payrollRunRepository.save(run);
        updatePayslipStatusByRun(runId, PayrollRunStatus.DISBURSED);
        return toRunResponse(saved, payrollEntryRepository.findByPayrollRun_IdAndActiveTrueOrderByStaff_IdAsc(runId));
    }

    @Override
    @Transactional
    public PayrollRunResponseDTO disburseRunByIdentifier(String identifier) {
        PayrollRun run = findActiveRunByIdentifier(identifier);
        return disburseRun(run.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayslipSummaryDTO> listPayslipsByRun(Long runId, Pageable pageable) {
        findActiveRun(runId);
        return payslipRepository.findByPayrollRun_IdAndActiveTrue(runId, pageable).map(this::toPayslipSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayslipSummaryDTO> listPayslipsByRunIdentifier(String identifier, Pageable pageable) {
        PayrollRun run = findActiveRunByIdentifier(identifier);
        return listPayslipsByRun(run.getId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipDetailDTO getPayslipById(Long payslipId) {
        Payslip payslip = payslipRepository.findByIdAndActiveTrue(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));

        return toPayslipDetail(payslip);
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipDetailDTO getPayslipByIdentifier(String identifier) {
        Payslip payslip = findActivePayslipByIdentifier(identifier);
        return toPayslipDetail(payslip);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayslipSummaryDTO> listMyPayslips(Pageable pageable) {
        Long currentStaffId = resolveCurrentStaffId();
        return payslipRepository.findByStaff_IdAndActiveTrueOrderByPayYearDescPayMonthDesc(currentStaffId, pageable)
                .map(this::toPayslipSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipDetailDTO getMyPayslipById(Long payslipId) {
        Long currentStaffId = resolveCurrentStaffId();
        Payslip payslip = payslipRepository.findByIdAndActiveTrue(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));
        if (!payslip.getStaff().getId().equals(currentStaffId)) {
            throw new EdusyncException("You are not allowed to access this payslip", HttpStatus.FORBIDDEN);
        }
        return toPayslipDetail(payslip);
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipDetailDTO getMyPayslipByIdentifier(String identifier) {
        Long currentStaffId = resolveCurrentStaffId();
        Payslip payslip = findActivePayslipByIdentifier(identifier);
        if (!payslip.getStaff().getId().equals(currentStaffId)) {
            throw new EdusyncException("You are not allowed to access this payslip", HttpStatus.FORBIDDEN);
        }
        return toPayslipDetail(payslip);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getMyPayslipPdf(Long payslipId) {
        PayslipDetailDTO payslip = getMyPayslipById(payslipId);
        Map<String, Object> data = new HashMap<>();
        data.put("payslip", payslip);
        data.put("generatedOn", LocalDateTime.now());
        return pdfGenerationService.generatePdfFromHtml("hrms/payslip", data);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getMyPayslipPdfByIdentifier(String identifier) {
        PayslipDetailDTO payslip = getMyPayslipByIdentifier(identifier);
        Map<String, Object> data = new HashMap<>();
        data.put("payslip", payslip);
        data.put("generatedOn", LocalDateTime.now());
        return pdfGenerationService.generatePdfFromHtml("hrms/payslip", data);
    }

    private PayslipDetailDTO toPayslipDetail(Payslip payslip) {
        Long payslipId = payslip.getId();
        List<PayslipLineItemDTO> items = payslipLineItemRepository.findByPayslip_IdAndActiveTrueOrderByIdAsc(payslipId)
                .stream()
                .map(item -> new PayslipLineItemDTO(
                        item.getComponentCode(),
                        item.getComponentName(),
                        item.getType().name(),
                        item.getAmount()
                ))
                .toList();

        Staff staff = payslip.getStaff();
        return new PayslipDetailDTO(
                payslip.getId(),
                payslip.getUuid() != null ? payslip.getUuid().toString() : null,
                payslip.getPayrollRun().getId(),
                staff.getId(),
                staffFullName(staff),
                staff.getEmployeeId(),
                payslip.getPayMonth(),
                payslip.getPayYear(),
                payslip.getTotalWorkingDays(),
                payslip.getDaysPresent(),
                payslip.getDaysAbsent(),
                payslip.getLopDays(),
                payslip.getGrossPay(),
                payslip.getTotalDeductions(),
                payslip.getNetPay(),
                payslip.getStatus(),
                payslip.getGeneratedAt(),
                items
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPayslipPdf(Long payslipId) {
        PayslipDetailDTO payslip = getPayslipById(payslipId);
        Map<String, Object> data = new HashMap<>();
        data.put("payslip", payslip);
        data.put("generatedOn", LocalDateTime.now());
        return pdfGenerationService.generatePdfFromHtml("hrms/payslip", data);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPayslipPdfByIdentifier(String identifier) {
        PayslipDetailDTO payslip = getPayslipByIdentifier(identifier);
        Map<String, Object> data = new HashMap<>();
        data.put("payslip", payslip);
        data.put("generatedOn", LocalDateTime.now());
        return pdfGenerationService.generatePdfFromHtml("hrms/payslip", data);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayslipSummaryDTO> listPayslipsByStaff(Long staffId, Pageable pageable) {
        return payslipRepository.findByStaff_IdAndActiveTrueOrderByPayYearDescPayMonthDesc(staffId, pageable)
                .map(this::toPayslipSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayslipSummaryDTO> listPayslipsByStaffIdentifier(String staffIdentifier, Pageable pageable) {
        Staff staff = findActiveStaffByIdentifier(staffIdentifier);
        return listPayslipsByStaff(staff.getId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollRunSummaryDTO> listRuns(Pageable pageable) {
        return payrollRunRepository.findByActiveTrueOrderByPayYearDescPayMonthDesc(pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollRunResponseDTO getRunById(Long runId) {
        PayrollRun run = findActiveRun(runId);

        List<PayrollEntry> entries = payrollEntryRepository.findByPayrollRun_IdAndActiveTrueOrderByStaff_IdAsc(runId);
        return toRunResponse(run, entries);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollRunResponseDTO getRunByIdentifier(String identifier) {
        PayrollRun run = findActiveRunByIdentifier(identifier);
        return getRunById(run.getId());
    }

    private PayrollRun findActiveRun(Long runId) {
        return payrollRunRepository.findByIdAndActiveTrue(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found with id: " + runId));
    }

    private PayrollRun findActiveRunByIdentifier(String identifier) {
        PayrollRun run = PublicIdentifierResolver.resolve(
                identifier,
                payrollRunRepository::findByUuid,
                payrollRunRepository::findById,
                "Payroll run"
        );
        if (!run.isActive()) {
            throw new ResourceNotFoundException("Payroll run not found with identifier: " + identifier);
        }
        return run;
    }

    private Payslip findActivePayslipByIdentifier(String identifier) {
        Payslip payslip = PublicIdentifierResolver.resolve(
                identifier,
                payslipRepository::findByUuid,
                payslipRepository::findById,
                "Payslip"
        );
        if (!payslip.isActive()) {
            throw new ResourceNotFoundException("Payslip not found with identifier: " + identifier);
        }
        return payslip;
    }

    private Staff findActiveStaffByIdentifier(String identifier) {
        Staff staff = PublicIdentifierResolver.resolve(
                identifier,
                staffRepository::findByUuid,
                staffRepository::findById,
                "Staff"
        );
        if (!staff.isActive()) {
            throw new ResourceNotFoundException("Staff not found with identifier: " + identifier);
        }
        return staff;
    }

    private PayrollRunSummaryDTO toSummary(PayrollRun run) {
        return new PayrollRunSummaryDTO(
                run.getId(),
                run.getUuid() != null ? run.getUuid().toString() : null,
                run.getPayYear(),
                run.getPayMonth(),
                run.getStatus(),
                run.getTotalStaff(),
                run.getTotalGross(),
                run.getTotalDeductions(),
                run.getTotalNet(),
                run.getProcessedOn()
        );
    }

    private PayslipSummaryDTO toPayslipSummary(Payslip payslip) {
        Staff staff = payslip.getStaff();
        return new PayslipSummaryDTO(
                payslip.getId(),
                payslip.getUuid() != null ? payslip.getUuid().toString() : null,
                payslip.getPayrollRun().getId(),
                staff.getId(),
                staffFullName(staff),
                staff.getEmployeeId(),
                payslip.getPayMonth(),
                payslip.getPayYear(),
                payslip.getGrossPay(),
                payslip.getTotalDeductions(),
                payslip.getNetPay(),
                payslip.getStatus(),
                payslip.getGeneratedAt()
        );
    }

    private PayrollRunResponseDTO toRunResponse(PayrollRun run, List<PayrollEntry> entries) {
        return new PayrollRunResponseDTO(
                run.getId(),
                run.getUuid() != null ? run.getUuid().toString() : null,
                run.getPayYear(),
                run.getPayMonth(),
                run.getStatus(),
                run.getRemarks(),
                run.getTotalStaff(),
                run.getTotalGross(),
                run.getTotalDeductions(),
                run.getTotalNet(),
                run.getProcessedOn(),
                entries.stream().map(this::toEntryResponse).toList()
        );
    }

    private PayrollRunEntryResponseDTO toEntryResponse(PayrollEntry entry) {
        Staff staff = entry.getStaff();
        return new PayrollRunEntryResponseDTO(
                entry.getId(),
                staff.getId(),
                staffFullName(staff),
                staff.getEmployeeId(),
                entry.getMapping().getId(),
                entry.getGrossPay(),
                entry.getTotalDeductions(),
                entry.getNetPay(),
                entry.getRemarks()
        );
    }

    private String staffFullName(Staff staff) {
        String first = staff.getUserProfile() != null && staff.getUserProfile().getFirstName() != null
                ? staff.getUserProfile().getFirstName()
                : "";
        String last = staff.getUserProfile() != null && staff.getUserProfile().getLastName() != null
                ? staff.getUserProfile().getLastName()
                : "";
        return (first + " " + last).trim();
    }

    private Payslip buildPayslip(
            PayrollRun run,
            StaffSalaryMapping mapping,
            ComputedSalaryBreakdownDTO computed,
            int totalWorkingDays,
            int presentDays,
            int absentDays,
            BigDecimal lopDays,
            BigDecimal adjustedDeductions,
            BigDecimal adjustedNet
    ) {
        Payslip payslip = new Payslip();
        payslip.setPayrollRun(run);
        payslip.setStaff(mapping.getStaff());
        payslip.setPayMonth(run.getPayMonth());
        payslip.setPayYear(run.getPayYear());
        payslip.setTotalWorkingDays(totalWorkingDays);
        payslip.setDaysAbsent(absentDays);
        payslip.setDaysPresent(presentDays);
        payslip.setLopDays(lopDays.setScale(2, RoundingMode.HALF_UP));
        payslip.setGrossPay(computed.grossPay());
        payslip.setTotalDeductions(adjustedDeductions);
        payslip.setNetPay(adjustedNet);
        payslip.setStatus(run.getStatus());
        payslip.setGeneratedAt(LocalDateTime.now());
        return payslip;
    }

    private void saveLineItems(Payslip payslip, List<ComputedComponentDTO> components, SalaryComponentType type) {
        for (ComputedComponentDTO component : components) {
            PayslipLineItem lineItem = new PayslipLineItem();
            lineItem.setPayslip(payslip);
            lineItem.setComponentCode(component.componentCode());
            lineItem.setComponentName(component.componentName());
            lineItem.setType(type);
            lineItem.setAmount(component.computedAmount());
            payslipLineItemRepository.save(lineItem);
        }
    }

    private void updatePayslipStatusByRun(Long runId, PayrollRunStatus status) {
        List<Payslip> payslips = payslipRepository.findByPayrollRun_IdAndActiveTrue(runId);
        for (Payslip payslip : payslips) {
            payslip.setStatus(status);
            payslipRepository.save(payslip);
        }
    }

    private String academicYearForDate(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return startYear + "-" + (startYear + 1);
    }

    private int countWorkingDays(LocalDate startDate, LocalDate endDate, String academicYear) {
        List<AcademicCalendarEvent> events = academicCalendarEventRepository
                .findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(academicYear, startDate, endDate);
        Map<LocalDate, DayType> dayTypeByDate = new HashMap<>();
        for (AcademicCalendarEvent event : events) {
            if (event.isAppliesToStaff()) {
                dayTypeByDate.put(event.getDate(), event.getDayType());
            }
        }

        int workingDays = 0;
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            DayType dayType = dayTypeByDate.get(cursor);
            if (dayType == null) {
                DayOfWeek dayOfWeek = cursor.getDayOfWeek();
                dayType = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
                        ? DayType.HOLIDAY
                        : DayType.WORKING;
            }

            if (dayType == DayType.WORKING || dayType == DayType.EXAM_DAY || dayType == DayType.HALF_DAY) {
                workingDays++;
            }
            cursor = cursor.plusDays(1);
        }
        return workingDays;
    }

    private AttendanceMetrics resolveAttendanceMetrics(Long staffId, LocalDate startDate, LocalDate endDate, int totalWorkingDays, BigDecimal lopDays) {
        int lopRounded = lopDays.setScale(0, RoundingMode.CEILING).intValue();
        long markedDays = staffDailyAttendanceRepository.countByStaffIdAndAttendanceDateBetween(staffId, startDate, endDate);
        if (markedDays > 0) {
            long present = staffDailyAttendanceRepository.countPresentByStaffIdAndDateBetween(staffId, startDate, endDate);
            long absent = staffDailyAttendanceRepository.countAbsentByStaffIdAndDateBetween(staffId, startDate, endDate);

            int presentDays = (int) Math.min(totalWorkingDays, Math.max(0L, present));
            int absentFromMarks = (int) Math.min(totalWorkingDays, Math.max(0L, absent));
            String policy = resolvePartialMarkPolicy();
            if (POLICY_MARKED_ONLY.equals(policy)) {
                int finalAbsent = Math.min(totalWorkingDays, Math.max(absentFromMarks, lopRounded));
                int finalPresent = Math.min(totalWorkingDays, Math.max(0, presentDays));
                return new AttendanceMetrics(finalPresent, finalAbsent);
            }

            int absentDerived = switch (policy) {
                case POLICY_FAIL_ON_PARTIAL -> {
                    if (markedDays < totalWorkingDays) {
                        throw new EdusyncException(
                                "Attendance is partially marked for payroll period; complete attendance marking before payroll run",
                                HttpStatus.BAD_REQUEST
                        );
                    }
                    yield absentFromMarks;
                }
                default -> Math.max(absentFromMarks, totalWorkingDays - presentDays);
            };
            int finalAbsent = Math.max(absentDerived, lopRounded);
            int finalPresent = Math.max(0, totalWorkingDays - finalAbsent);
            return new AttendanceMetrics(finalPresent, finalAbsent);
        }

        int absentDays = Math.min(totalWorkingDays, Math.max(0, lopRounded));
        int presentDays = Math.max(0, totalWorkingDays - absentDays);
        return new AttendanceMetrics(presentDays, absentDays);
    }

    private String resolvePartialMarkPolicy() {
        if (partialMarkPolicy == null || partialMarkPolicy.isBlank()) {
            return POLICY_TREAT_UNMARKED_AS_ABSENT;
        }

        String normalized = partialMarkPolicy.trim().toUpperCase();
        if (POLICY_MARKED_ONLY.equals(normalized) || POLICY_FAIL_ON_PARTIAL.equals(normalized)) {
            return normalized;
        }
        return POLICY_TREAT_UNMARKED_AS_ABSENT;
    }

    private record AttendanceMetrics(int presentDays, int absentDays) {
    }

    private Long resolveCurrentStaffId() {
        Long currentUserId = authUtil.getCurrentUserId();
        return staffRepository.findByUserProfile_User_Id(currentUserId)
                .orElseThrow(() -> new EdusyncException("Authenticated user is not linked to a staff profile", HttpStatus.FORBIDDEN))
                .getId();
    }
}










