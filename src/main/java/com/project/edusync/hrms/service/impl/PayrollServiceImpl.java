package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.settings.service.AppSettingService;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.ams.model.dto.request.StaffAttendanceRequestDTO;
import com.project.edusync.ams.model.dto.response.AttendanceCompletionDTO;
import com.project.edusync.ams.model.enums.AttendanceSource;
import com.project.edusync.ams.model.service.StaffAttendanceService;
import com.project.edusync.hrms.dto.payroll.BankAdviceStaffEntryDTO;
import com.project.edusync.hrms.dto.payroll.BankSalaryAdviceDTO;
import com.project.edusync.hrms.dto.payroll.PayrollPreflightDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunCreateDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunEntryResponseDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunResponseDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunSummaryDTO;
import com.project.edusync.hrms.dto.payroll.PayslipDetailDTO;
import com.project.edusync.hrms.dto.payroll.PayslipLineItemDTO;
import com.project.edusync.hrms.dto.payroll.PayslipSummaryDTO;
import com.project.edusync.hrms.dto.payroll.StaffAttendanceSummaryDTO;
import com.project.edusync.hrms.dto.salary.ComputedComponentDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.model.entity.LeaveApplication;
import com.project.edusync.hrms.model.entity.Payslip;
import com.project.edusync.hrms.model.entity.PayslipLineItem;
import com.project.edusync.hrms.model.entity.AcademicCalendarEvent;
import com.project.edusync.hrms.model.entity.PayrollEntry;
import com.project.edusync.hrms.model.entity.PayrollRun;
import com.project.edusync.hrms.model.entity.StaffSalaryMapping;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.model.enums.SalaryComponentType;
import com.project.edusync.hrms.model.entity.LoanRepaymentRecord;
import com.project.edusync.hrms.model.entity.StaffLoan;
import com.project.edusync.hrms.model.enums.LoanStatus;
import com.project.edusync.hrms.model.enums.RepaymentStatus;
import com.project.edusync.hrms.repository.LoanRepaymentRecordRepository;
import com.project.edusync.hrms.repository.StaffLoanRepository;
import com.project.edusync.ams.model.entity.StaffDailyAttendance;
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
import com.project.edusync.uis.model.entity.StaffSensitiveInfo;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StaffSensitiveInfoRepository;
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
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.math.RoundingMode;
import java.util.Set;

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
    private final StaffAttendanceService staffAttendanceService;
    private final StaffLoanRepository loanRepository;
    private final LoanRepaymentRecordRepository loanRepaymentRepository;
    private final StaffSensitiveInfoRepository staffSensitiveInfoRepository;
    private final AppSettingService appSettingService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.project.edusync.hrms.repository.OvertimeRecordRepository overtimeRecordRepository;

    @Value("${app.hrms.payroll.attendance.partial-mark-policy:TREAT_UNMARKED_AS_ABSENT}")
    private String partialMarkPolicy;

    @Override
    @Transactional
    public PayrollRunResponseDTO createRun(PayrollRunCreateDTO dto) {
        PayrollPreflightDTO preflight = getPayrollPreflight(dto.payYear(), dto.payMonth());
        if (!preflight.canProcess()) {
            throw new EdusyncException("Payroll preflight failed: " + preflight.blockers(), HttpStatus.BAD_REQUEST);
        }

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

            // Deduct scheduled loan EMIs due this month
            BigDecimal loanEmiDeduction = deductLoanEmis(mapping.getStaff().getId(), savedRun.getUuid(), rangeStart);
            adjustedDeductions = adjustedDeductions.add(loanEmiDeduction).setScale(2, RoundingMode.HALF_UP);

            // Add Cash Overtime
            List<com.project.edusync.hrms.model.entity.OvertimeRecord> unprocessedOts = overtimeRecordRepository.findByStaff_IdAndStatusAndCompensationTypeAndWorkDateBetweenAndActiveTrue(
                    mapping.getStaff().getId(),
                    com.project.edusync.hrms.model.enums.OvertimeStatus.APPROVED,
                    "CASH",
                    rangeStart,
                    rangeEnd
            );

            BigDecimal totalOtEarning = BigDecimal.ZERO;
            for (com.project.edusync.hrms.model.entity.OvertimeRecord ot : unprocessedOts) {
                if (ot.getApprovedAmount() != null) {
                    totalOtEarning = totalOtEarning.add(ot.getApprovedAmount());
                }
                ot.setStatus(com.project.edusync.hrms.model.enums.OvertimeStatus.CONVERTED);
                ot.setPayrollRunRef(savedRun.getUuid());
                overtimeRecordRepository.save(ot);
            }

            BigDecimal adjustedGrossPay = computed.grossPay().add(totalOtEarning).setScale(2, RoundingMode.HALF_UP);
            BigDecimal adjustedNet = adjustedGrossPay.subtract(adjustedDeductions).setScale(2, RoundingMode.HALF_UP);

            // We need to inject the Overtime component into the earnings list for display
            List<ComputedComponentDTO> augmentedEarnings = new ArrayList<>(computed.earnings());
            if (totalOtEarning.compareTo(BigDecimal.ZERO) > 0) {
                augmentedEarnings.add(new ComputedComponentDTO(
                        "OVERTIME_PAY",
                        "Overtime Payout",
                        "FLAT",
                        totalOtEarning,
                        totalOtEarning,
                        false
                ));
            }

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
            entry.setGrossPay(adjustedGrossPay);
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
                    adjustedGrossPay,
                    adjustedDeductions,
                    adjustedNet
            );
            Payslip savedPayslip = payslipRepository.save(payslip);
            saveLineItems(savedPayslip, augmentedEarnings, SalaryComponentType.EARNING);
            saveLineItems(savedPayslip, computed.deductions(), SalaryComponentType.DEDUCTION);

            // Save loan EMI deductions as a payslip line item so they appear on payslips/bank advice
            if (loanEmiDeduction.compareTo(BigDecimal.ZERO) > 0) {
                PayslipLineItem loanLine = new PayslipLineItem();
                loanLine.setPayslip(savedPayslip);
                loanLine.setComponentCode("LOAN_EMI");
                loanLine.setComponentName("Loan EMI");
                loanLine.setType(SalaryComponentType.DEDUCTION);
                loanLine.setAmount(loanEmiDeduction);
                payslipLineItemRepository.save(loanLine);
            }

            totalGross = totalGross.add(adjustedGrossPay);
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

    @Override
    @Transactional(readOnly = true)
    public StaffAttendanceSummaryDTO getMyAttendanceSummary(int year, int month) {
        if (month < 1 || month > 12) {
            throw new EdusyncException("Month must be between 1 and 12", HttpStatus.BAD_REQUEST);
        }

        Long staffId = resolveCurrentStaffId();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        String academicYear = academicYearForDate(start);

        List<AcademicCalendarEvent> events = academicCalendarEventRepository
                .findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(academicYear, start, end);
        Map<LocalDate, DayType> dayTypeByDate = new HashMap<>();
        for (AcademicCalendarEvent event : events) {
            if (event.isAppliesToStaff()) {
                dayTypeByDate.put(event.getDate(), event.getDayType());
            }
        }

        List<StaffDailyAttendance> attendanceRecords = staffDailyAttendanceRepository
                .findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(staffId, start, end);
        Map<LocalDate, StaffDailyAttendance> attendanceByDate = new HashMap<>();
        for (StaffDailyAttendance record : attendanceRecords) {
            attendanceByDate.put(record.getAttendanceDate(), record);
        }

        List<LeaveApplication> approvedLeaves = leaveApplicationRepository.findApprovedActiveByStaffIdAndDateRange(staffId, start, end);
        Set<LocalDate> leaveDates = new HashSet<>();
        for (LeaveApplication leave : approvedLeaves) {
            LocalDate cursor = leave.getFromDate().isBefore(start) ? start : leave.getFromDate();
            LocalDate stop = leave.getToDate().isAfter(end) ? end : leave.getToDate();
            while (!cursor.isAfter(stop)) {
                leaveDates.add(cursor);
                cursor = cursor.plusDays(1);
            }
        }

        int presentDays = 0;
        int absentDays = 0;
        int leaveDays = 0;
        int holidays = 0;
        List<StaffAttendanceSummaryDTO.DayRecord> dailyRecords = new ArrayList<>();

        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            DayType dayType = dayTypeByDate.get(cursor);
            boolean weekend = cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean holiday = isHoliday(dayType, weekend);

            String status;
            StaffDailyAttendance attendance = attendanceByDate.get(cursor);
            if (attendance != null && attendance.getAttendanceType() != null) {
                if (attendance.getAttendanceType().isPresentMark()) {
                    status = "PRESENT";
                    presentDays++;
                } else if (attendance.getAttendanceType().isAbsenceMark()) {
                    status = "ABSENT";
                    absentDays++;
                } else {
                    status = holiday ? (weekend && dayType == null ? "WEEKEND" : "HOLIDAY") : "ABSENT";
                    if (holiday) {
                        holidays++;
                    } else {
                        absentDays++;
                    }
                }
            } else if (leaveDates.contains(cursor)) {
                status = "LEAVE";
                leaveDays++;
            } else if (holiday) {
                status = weekend && dayType == null ? "WEEKEND" : "HOLIDAY";
                holidays++;
            } else {
                status = "ABSENT";
                absentDays++;
            }

            dailyRecords.add(StaffAttendanceSummaryDTO.DayRecord.builder()
                    .date(cursor)
                    .status(status)
                    .build());
            cursor = cursor.plusDays(1);
        }

        int totalDays = yearMonth.lengthOfMonth();
        int workingDays = Math.max(totalDays - holidays, 0);
        BigDecimal percentage = workingDays == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(presentDays)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(workingDays), 1, RoundingMode.HALF_UP);

        String periodLabel = Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + year;
        return StaffAttendanceSummaryDTO.builder()
                .periodLabel(periodLabel)
                .totalDays(totalDays)
                .presentDays(presentDays)
                .absentDays(absentDays)
                .leaveDays(leaveDays)
                .holidays(holidays)
                .attendancePercentage(percentage)
                .dailyRecords(dailyRecords)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollPreflightDTO getPayrollPreflight(int year, int month) {
        if (month < 1 || month > 12) {
            throw new EdusyncException("Month must be between 1 and 12", HttpStatus.BAD_REQUEST);
        }

        AttendanceCompletionDTO completion = staffAttendanceService.getAttendanceCompletion(month, year);

        boolean attendanceComplete = completion.completionPercentage() >= 100.0;
        List<PayrollPreflightDTO.PayrollBlockerDTO> blockers = new ArrayList<>();
        if (!attendanceComplete) {
            List<Map<String, Object>> unmarked = completion.unmarkedStaff().stream()
                    .map(s -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("staffName", s.staffName());
                        item.put("employeeId", s.employeeId());
                        item.put("missingDays", s.missingDates() == null ? 0 : s.missingDates().size());
                        return item;
                    })
                    .toList();

            blockers.add(PayrollPreflightDTO.PayrollBlockerDTO.builder()
                    .type("INCOMPLETE_ATTENDANCE")
                    .message(completion.unmarkedStaff().size() + " staff members have unmarked attendance")
                    .details(Map.of(
                            "totalActiveStaff", completion.totalActiveStaff(),
                            "totalWorkingDays", completion.totalWorkingDays(),
                            "completionPercentage", completion.completionPercentage(),
                            "unmarkedStaff", unmarked
                    ))
                    .build());
        }

        // Check for unmapped staff (no salary template assigned)
        LocalDate snapshotDate = YearMonth.of(year, month).atEndOfMonth();
        long totalStaff = staffRepository.countByIsActiveTrue();
        long withSalary = staffSalaryMappingRepository.countDistinctStaffWithActiveMappingOnDate(snapshotDate, LocalDate.of(9999, 12, 31));
        long unmappedCount = Math.max(totalStaff - withSalary, 0);
        if (unmappedCount > 0) {
            blockers.add(PayrollPreflightDTO.PayrollBlockerDTO.builder()
                    .type("UNMAPPED_STAFF")
                    .message(unmappedCount + " staff member(s) have no salary template assigned")
                    .details(Map.of("unmappedCount", unmappedCount))
                    .build());
        }

        // Check duplicate run
        boolean alreadyProcessed = payrollRunRepository.existsByPayYearAndPayMonthAndActiveTrue(year, month);
        if (alreadyProcessed) {
            blockers.add(PayrollPreflightDTO.PayrollBlockerDTO.builder()
                    .type("DUPLICATE_RUN")
                    .message("A payroll run already exists for this period")
                    .details(Map.of())
                    .build());
        }

        int pendingLeaves = (int) leaveApplicationRepository.countByActiveTrueAndStatus(LeaveApplicationStatus.PENDING);

        // Check for staff with active salary mappings who are missing bank details
        List<Long> mappedStaffIds = staffSalaryMappingRepository.findCurrentMappedStaffIds(LocalDate.now());
        long missingBankDetails = mappedStaffIds.stream()
                .filter(staffId -> {
                    var info = staffSensitiveInfoRepository.findByStaff_Id(staffId).orElse(null);
                    return info == null
                            || info.getBankAccountNumber() == null || info.getBankAccountNumber().isBlank()
                            || info.getBankIfscCode() == null || info.getBankIfscCode().isBlank();
                })
                .count();

        List<PayrollPreflightDTO.PayrollWarningDTO> warnings = new java.util.ArrayList<>();
        if (pendingLeaves > 0) {
            warnings.add(PayrollPreflightDTO.PayrollWarningDTO.builder()
                    .type("PENDING_LEAVE_APPLICATIONS")
                    .message(pendingLeaves + " leave applications are still pending approval")
                    .count(pendingLeaves)
                    .build());
        }
        if (missingBankDetails > 0) {
            warnings.add(PayrollPreflightDTO.PayrollWarningDTO.builder()
                    .type("MISSING_BANK_DETAILS")
                    .message(missingBankDetails + " staff member(s) are missing bank account or IFSC details")
                    .count((int) missingBankDetails)
                    .build());
        }

        PayrollPreflightDTO.PayrollPreflightSummaryDTO summary = PayrollPreflightDTO.PayrollPreflightSummaryDTO.builder()
                .totalStaff(totalStaff)
                .staffWithSalaryMapping(withSalary)
                .staffWithoutSalaryMapping(unmappedCount)
                .totalApprovedLeaves(leaveApplicationRepository.countByActiveTrueAndStatus(LeaveApplicationStatus.APPROVED))
                .totalLopDays(0)
                .attendanceCompletionPercent(completion.completionPercentage())
                .build();

        return PayrollPreflightDTO.builder()
                .month(month)
                .year(year)
                .canProcess(attendanceComplete && !alreadyProcessed && unmappedCount == 0)
                .alreadyProcessed(alreadyProcessed)
                .blockers(blockers)
                .warnings(warnings)
                .summary(summary)
                .build();
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
            BigDecimal adjustedGrossPay,
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
        payslip.setGrossPay(adjustedGrossPay);
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

    private BigDecimal deductLoanEmis(Long staffId, java.util.UUID payrollRunRef, LocalDate monthStart) {
        List<StaffLoan> activeLoans = loanRepository.findByStaff_IdAndStatusIn(
                staffId,
                List.of(LoanStatus.ACTIVE, LoanStatus.DISBURSED)
        );
        if (activeLoans.isEmpty()) {
            return BigDecimal.ZERO;
        }

        YearMonth targetMonth = YearMonth.from(monthStart);
        BigDecimal total = BigDecimal.ZERO;

        for (StaffLoan loan : activeLoans) {
            List<LoanRepaymentRecord> scheduled = loanRepaymentRepository.findByLoan_IdAndStatus(
                    loan.getId(),
                    RepaymentStatus.SCHEDULED
            );

            for (LoanRepaymentRecord repayment : scheduled) {
                if (repayment.getDueDate() == null || !YearMonth.from(repayment.getDueDate()).equals(targetMonth)) {
                    continue;
                }
                repayment.setStatus(RepaymentStatus.DEDUCTED);
                repayment.setPaidDate(monthStart.withDayOfMonth(monthStart.lengthOfMonth()));
                repayment.setPayrollRunRef(payrollRunRef);
                loanRepaymentRepository.save(repayment);

                total = total.add(repayment.getAmount() != null ? repayment.getAmount() : BigDecimal.ZERO);

                if (loan.getRemainingEmis() != null && loan.getRemainingEmis() > 0) {
                    loan.setRemainingEmis(loan.getRemainingEmis() - 1);
                }
            }

            if (loan.getRemainingEmis() != null && loan.getRemainingEmis() <= 0) {
                loan.setRemainingEmis(0);
                loan.setStatus(LoanStatus.CLOSED);
            }
            loanRepository.save(loan);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
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

    private boolean isHoliday(DayType dayType, boolean weekend) {
        if (dayType != null) {
            return dayType == DayType.HOLIDAY
                    || dayType == DayType.VACATION
                    || dayType == DayType.RESTRICTED_HOLIDAY;
        }
        return weekend;
    }

    private record AttendanceMetrics(int presentDays, int absentDays) {
    }

    private Long resolveCurrentStaffId() {
        Long currentUserId = authUtil.getCurrentUserId();
        return staffRepository.findByUserProfile_User_Id(currentUserId)
                .orElseThrow(() -> new EdusyncException("Authenticated user is not linked to a staff profile", HttpStatus.FORBIDDEN))
                .getId();
    }

    // ── Bank Salary Advice ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BankSalaryAdviceDTO getBankSalaryAdvice(String runIdentifier) {
        PayrollRun run = findActiveRunByIdentifier(runIdentifier);

        if (run.getStatus() == PayrollRunStatus.PROCESSED) {
            throw new EdusyncException(
                    "Bank Salary Advice is only available after the payroll run is APPROVED or DISBURSED.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        List<Payslip> payslips = payslipRepository.findByPayrollRun_IdAndActiveTrue(run.getId());
        payslips.sort(java.util.Comparator.comparing(p -> p.getStaff().getEmployeeId()));

        // Deduction aggregate map: code → (name, running total)
        Map<String, BigDecimal> deductionTotalsMap = new TreeMap<>();
        Map<String, String> deductionNameMap = new LinkedHashMap<>();

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        List<BankAdviceStaffEntryDTO> entries = new ArrayList<>();
        int serial = 1;

        for (Payslip payslip : payslips) {
            Staff staff = payslip.getStaff();
            StaffSensitiveInfo sensitive = staffSensitiveInfoRepository
                    .findByStaff_Id(staff.getId())
                    .orElse(null);

            String bankName = sensitive != null ? sensitive.getBankName() : null;
            String accountNumber = sensitive != null ? sensitive.getBankAccountNumber() : null;
            String ifscCode = sensitive != null ? sensitive.getBankIfscCode() : null;

            List<PayslipLineItemDTO> allItems = payslipLineItemRepository
                    .findByPayslip_IdAndActiveTrueOrderByIdAsc(payslip.getId())
                    .stream()
                    .map(item -> new PayslipLineItemDTO(
                            item.getComponentCode(),
                            item.getComponentName(),
                            item.getType().name(),
                            item.getAmount()))
                    .toList();

            List<PayslipLineItemDTO> earningLines = allItems.stream()
                    .filter(i -> "EARNING".equals(i.type()))
                    .toList();
            List<PayslipLineItemDTO> deductionLines = allItems.stream()
                    .filter(i -> "DEDUCTION".equals(i.type()))
                    .toList();

            // Accumulate deduction component totals for annexure
            for (PayslipLineItemDTO line : deductionLines) {
                deductionTotalsMap.merge(line.componentCode(), line.amount(), BigDecimal::add);
                deductionNameMap.putIfAbsent(line.componentCode(), line.componentName());
            }

            String designation = staff.getDesignation() != null
                    ? staff.getDesignation().getDesignationName()
                    : staff.getJobTitle();
            String department = staff.getDepartment() != null
                    ? staff.getDepartment().name()
                    : null;

            entries.add(new BankAdviceStaffEntryDTO(
                    serial++,
                    staff.getEmployeeId(),
                    staffFullName(staff),
                    designation,
                    department,
                    bankName,
                    accountNumber,
                    ifscCode,
                    payslip.getGrossPay(),
                    payslip.getTotalDeductions(),
                    payslip.getNetPay(),
                    earningLines,
                    deductionLines
            ));

            totalGross = totalGross.add(payslip.getGrossPay());
            totalDeductions = totalDeductions.add(payslip.getTotalDeductions());
            totalNet = totalNet.add(payslip.getNetPay());
        }

        List<BankSalaryAdviceDTO.DeductionAggregateLine> deductionAggregates = deductionTotalsMap.entrySet()
                .stream()
                .map(e -> new BankSalaryAdviceDTO.DeductionAggregateLine(
                        e.getKey(),
                        deductionNameMap.getOrDefault(e.getKey(), e.getKey()),
                        e.getValue()))
                .toList();

        String payPeriodLabel = Month.of(run.getPayMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + run.getPayYear();

        return new BankSalaryAdviceDTO(
                runIdentifier,
                run.getPayMonth(),
                run.getPayYear(),
                payPeriodLabel,
                LocalDateTime.now(),
                appSettingService.getValue("school.name", "Institution"),
                entries.size(),
                totalGross,
                totalDeductions,
                totalNet,
                entries,
                deductionAggregates
        );
    }

    @Override
    @Transactional
    public int markAllAbsentForPeriod(int year, int month) {
        AttendanceCompletionDTO completion = staffAttendanceService.getAttendanceCompletion(month, year);
        List<AttendanceCompletionDTO.UnmarkedStaffAttendanceDTO> unmarked = completion.unmarkedStaff();
        if (unmarked == null || unmarked.isEmpty()) {
            return 0;
        }

        Long performedBy = authUtil.getCurrentUserId();
        List<StaffAttendanceRequestDTO> requests = new ArrayList<>();

        for (AttendanceCompletionDTO.UnmarkedStaffAttendanceDTO entry : unmarked) {
            if (entry.staffUuid() == null || entry.missingDates() == null) continue;
            java.util.UUID staffUuid = java.util.UUID.fromString(entry.staffUuid());
            for (LocalDate date : entry.missingDates()) {
                requests.add(new StaffAttendanceRequestDTO(
                        staffUuid, date, "A", null, null, null,
                        AttendanceSource.MANUAL, "Auto-marked absent via payroll preflight", null, null
                ));
            }
        }

        if (requests.isEmpty()) return 0;
        staffAttendanceService.bulkCreate(requests, performedBy);
        return requests.size();
    }

    @Override
    @Transactional
    public int markAllPresentForPeriod(int year, int month) {
        AttendanceCompletionDTO completion = staffAttendanceService.getAttendanceCompletion(month, year);
        List<AttendanceCompletionDTO.UnmarkedStaffAttendanceDTO> unmarked = completion.unmarkedStaff();
        if (unmarked == null || unmarked.isEmpty()) {
            return 0;
        }

        Long performedBy = authUtil.getCurrentUserId();
        List<StaffAttendanceRequestDTO> requests = new ArrayList<>();

        for (AttendanceCompletionDTO.UnmarkedStaffAttendanceDTO entry : unmarked) {
            if (entry.staffUuid() == null || entry.missingDates() == null) continue;
            java.util.UUID staffUuid = java.util.UUID.fromString(entry.staffUuid());
            for (LocalDate date : entry.missingDates()) {
                requests.add(new StaffAttendanceRequestDTO(
                        staffUuid, date, "P", null, null, null,
                        AttendanceSource.MANUAL, "Auto-marked present via payroll preflight", null, null
                ));
            }
        }

        if (requests.isEmpty()) return 0;
        staffAttendanceService.bulkCreate(requests, performedBy);
        return requests.size();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getBankSalaryAdvicePdf(String runIdentifier) {
        BankSalaryAdviceDTO advice = getBankSalaryAdvice(runIdentifier);
        Map<String, Object> data = new HashMap<>();
        data.put("advice", advice);
        return pdfGenerationService.generatePdfFromHtml("hrms/bank-salary-advice", data);
    }

    @Override
    @Transactional
    public PayrollRunResponseDTO voidRun(String identifier) {
        PayrollRun run = findActiveRunByIdentifier(identifier);

        if (run.getStatus() == PayrollRunStatus.DISBURSED) {
            throw new EdusyncException(
                    "Cannot void a DISBURSED payroll run. Salaries have already been paid.",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (run.getStatus() == PayrollRunStatus.VOIDED) {
            throw new EdusyncException("This payroll run is already voided.",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        // 1. Revert loan repayment records DEDUCTED by this run → back to SCHEDULED
        List<Payslip> payslips = payslipRepository.findByPayrollRun_IdAndActiveTrue(run.getId());

        // Revert loan repayments by payrollRunRef directly
        List<com.project.edusync.hrms.model.entity.LoanRepaymentRecord> allDeducted =
                loanRepaymentRepository.findByPayrollRunRef(run.getUuid());
        for (com.project.edusync.hrms.model.entity.LoanRepaymentRecord repayment : allDeducted) {
            repayment.setStatus(RepaymentStatus.SCHEDULED);
            repayment.setPaidDate(null);
            repayment.setPayrollRunRef(null);
            loanRepaymentRepository.save(repayment);

            // Restore loan remaining EMI count and reopen if CLOSED
            com.project.edusync.hrms.model.entity.StaffLoan loan = repayment.getLoan();
            if (loan != null) {
                if (loan.getRemainingEmis() != null) {
                    loan.setRemainingEmis(loan.getRemainingEmis() + 1);
                }
                if (loan.getStatus() == LoanStatus.CLOSED) {
                    loan.setStatus(LoanStatus.ACTIVE);
                }
                loanRepository.save(loan);
            }
        }

        // 2. Revert overtime records converted by this run → back to APPROVED
        List<com.project.edusync.hrms.model.entity.OvertimeRecord> convertedOts =
                overtimeRecordRepository.findByPayrollRunRefAndStatusAndActiveTrue(
                        run.getUuid(),
                        com.project.edusync.hrms.model.enums.OvertimeStatus.CONVERTED
                );
        for (com.project.edusync.hrms.model.entity.OvertimeRecord ot : convertedOts) {
            ot.setStatus(com.project.edusync.hrms.model.enums.OvertimeStatus.APPROVED);
            ot.setPayrollRunRef(null);
            overtimeRecordRepository.save(ot);
        }

        // 3. Soft-delete payslip line items, payslips, and payroll entries
        for (Payslip payslip : payslips) {
            List<com.project.edusync.hrms.model.entity.PayslipLineItem> lineItems =
                    payslipLineItemRepository.findByPayslip_IdAndActiveTrueOrderByIdAsc(payslip.getId());
            for (com.project.edusync.hrms.model.entity.PayslipLineItem li : lineItems) {
                li.setActive(false);
                payslipLineItemRepository.save(li);
            }
            payslip.setActive(false);
            payslipRepository.save(payslip);
        }

        List<PayrollEntry> entries = payrollEntryRepository
                .findByPayrollRun_IdAndActiveTrueOrderByStaff_IdAsc(run.getId());
        for (PayrollEntry entry : entries) {
            entry.setActive(false);
            payrollEntryRepository.save(entry);
        }

        // 4. Mark run as VOIDED (keep the record for audit; soft delete guard lifted by setting active=false)
        run.setStatus(PayrollRunStatus.VOIDED);
        run.setActive(false);
        PayrollRun saved = payrollRunRepository.save(run);

        return toRunResponse(saved, List.of());
    }
}










