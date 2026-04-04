package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.hrms.dto.payroll.PayrollRunCreateDTO;
import com.project.edusync.hrms.dto.payroll.PayslipDetailDTO;
import com.project.edusync.hrms.dto.payroll.PayslipSummaryDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunResponseDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.model.entity.PayrollEntry;
import com.project.edusync.hrms.model.entity.PayrollRun;
import com.project.edusync.hrms.model.entity.Payslip;
import com.project.edusync.hrms.model.entity.PayslipLineItem;
import com.project.edusync.hrms.model.entity.StaffSalaryMapping;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.repository.PayslipLineItemRepository;
import com.project.edusync.hrms.repository.PayslipRepository;
import com.project.edusync.hrms.repository.PayrollEntryRepository;
import com.project.edusync.hrms.repository.PayrollRunRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
import com.project.edusync.hrms.service.StaffSalaryMappingService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceImplTest {

    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private PayrollEntryRepository payrollEntryRepository;
    @Mock
    private PayslipRepository payslipRepository;
    @Mock
    private PayslipLineItemRepository payslipLineItemRepository;
    @Mock
    private AcademicCalendarEventRepository academicCalendarEventRepository;
    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;
    @Mock
    private StaffSalaryMappingRepository staffSalaryMappingRepository;
    @Mock
    private StaffSalaryMappingService staffSalaryMappingService;
    @Mock
    private PdfGenerationService pdfGenerationService;
    @Mock
    private AuthUtil authUtil;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private StaffDailyAttendanceRepository staffDailyAttendanceRepository;

    @InjectMocks
    private PayrollServiceImpl service;

    @Test
    void createRunBuildsEntriesAndTotals() {
        Staff staff = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        StaffSalaryMapping mapping = buildMapping(1001L, staff);

        when(payrollRunRepository.existsByPayYearAndPayMonthAndActiveTrue(2026, 4)).thenReturn(false);
        when(staffSalaryMappingRepository.findActiveMappingsEffectiveInRange(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(9999, 12, 31)
        )).thenReturn(List.of(mapping));
        when(academicCalendarEventRepository.findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(
                "2026-2027",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of());
        when(leaveApplicationRepository.sumApprovedLopDaysOverlapping(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(BigDecimal.ZERO);
        when(staffDailyAttendanceRepository.countByStaffIdAndAttendanceDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(0L);

        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(invocation -> {
            PayrollRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(500L);
            }
            if (run.getProcessedOn() == null) {
                run.setProcessedOn(LocalDateTime.now());
            }
            run.setStatus(PayrollRunStatus.PROCESSED);
            return run;
        });

        when(staffSalaryMappingService.computeBreakdown(1001L)).thenReturn(new ComputedSalaryBreakdownDTO(
                101L,
                "Ravi Kumar",
                "EMP001",
                "Template A",
                "PRT",
                List.of(),
                List.of(),
                new BigDecimal("16800.00"),
                new BigDecimal("1440.00"),
                new BigDecimal("15360.00"),
                new BigDecimal("16800.00"),
                false
        ));

        when(payrollEntryRepository.save(any(PayrollEntry.class))).thenAnswer(invocation -> {
            PayrollEntry entry = invocation.getArgument(0);
            entry.setId(7001L);
            return entry;
        });
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(invocation -> {
            Payslip payslip = invocation.getArgument(0);
            if (payslip.getId() == null) {
                payslip.setId(9001L);
            }
            return payslip;
        });
        PayrollRunResponseDTO response = service.createRun(new PayrollRunCreateDTO(2026, 4, "April payroll"));

        assertEquals(500L, response.runId());
        assertEquals(1, response.totalStaff());
        assertEquals(new BigDecimal("16800.00"), response.totalGross());
        assertEquals(new BigDecimal("1440.00"), response.totalDeductions());
        assertEquals(new BigDecimal("15360.00"), response.totalNet());
        assertEquals(1, response.entries().size());
        assertEquals("EMP001", response.entries().get(0).employeeId());
    }

    @Test
    void createRunThrowsConflictOnDuplicateMonth() {
        when(payrollRunRepository.existsByPayYearAndPayMonthAndActiveTrue(2026, 4)).thenReturn(true);
        assertThrows(EdusyncException.class, () -> service.createRun(new PayrollRunCreateDTO(2026, 4, null)));
    }

    @Test
    void getRunByIdThrowsWhenMissing() {
        when(payrollRunRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getRunById(99L));
    }

    @Test
    void approveRunMovesStatusToApproved() {
        PayrollRun run = new PayrollRun();
        run.setId(500L);
        run.setActive(true);
        run.setPayYear(2026);
        run.setPayMonth(4);
        run.setStatus(PayrollRunStatus.PROCESSED);
        run.setProcessedOn(LocalDateTime.now());
        run.setTotalStaff(0);

        when(payrollRunRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(run));
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payrollEntryRepository.findByPayrollRun_IdAndActiveTrueOrderByStaff_IdAsc(500L)).thenReturn(List.of());
        when(payslipRepository.findByPayrollRun_IdAndActiveTrue(500L)).thenReturn(List.of());

        PayrollRunResponseDTO response = service.approveRun(500L);
        assertEquals(PayrollRunStatus.APPROVED, response.status());
    }

    @Test
    void disburseRunThrowsWhenRunNotApproved() {
        PayrollRun run = new PayrollRun();
        run.setId(501L);
        run.setActive(true);
        run.setStatus(PayrollRunStatus.PROCESSED);

        when(payrollRunRepository.findByIdAndActiveTrue(501L)).thenReturn(Optional.of(run));
        assertThrows(EdusyncException.class, () -> service.disburseRun(501L));
    }

    @Test
    void listPayslipsByRunReturnsPagedData() {
        PayrollRun run = new PayrollRun();
        run.setId(500L);
        run.setActive(true);

        Staff staff = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        Payslip payslip = new Payslip();
        payslip.setId(9001L);
        payslip.setPayrollRun(run);
        payslip.setStaff(staff);
        payslip.setPayMonth(4);
        payslip.setPayYear(2026);
        payslip.setGrossPay(new BigDecimal("16800.00"));
        payslip.setTotalDeductions(new BigDecimal("1440.00"));
        payslip.setNetPay(new BigDecimal("15360.00"));
        payslip.setStatus(PayrollRunStatus.PROCESSED);
        payslip.setGeneratedAt(LocalDateTime.now());

        when(payrollRunRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(run));
        when(payslipRepository.findByPayrollRun_IdAndActiveTrue(any(), any()))
                .thenReturn(new PageImpl<>(List.of(payslip), PageRequest.of(0, 20), 1));

        org.springframework.data.domain.Page<PayslipSummaryDTO> result = service.listPayslipsByRun(500L, PageRequest.of(0, 20));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPayslipByIdReturnsLineItems() {
        PayrollRun run = new PayrollRun();
        run.setId(500L);

        Staff staff = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        Payslip payslip = new Payslip();
        payslip.setId(9001L);
        payslip.setPayrollRun(run);
        payslip.setStaff(staff);
        payslip.setPayMonth(4);
        payslip.setPayYear(2026);
        payslip.setTotalWorkingDays(26);
        payslip.setDaysPresent(25);
        payslip.setDaysAbsent(1);
        payslip.setLopDays(new BigDecimal("0.00"));
        payslip.setGrossPay(new BigDecimal("16800.00"));
        payslip.setTotalDeductions(new BigDecimal("1440.00"));
        payslip.setNetPay(new BigDecimal("15360.00"));
        payslip.setStatus(PayrollRunStatus.PROCESSED);
        payslip.setGeneratedAt(LocalDateTime.now());

        PayslipLineItem item = new PayslipLineItem();
        item.setComponentCode("BASIC");
        item.setComponentName("Basic Pay");
        item.setType(com.project.edusync.hrms.model.enums.SalaryComponentType.EARNING);
        item.setAmount(new BigDecimal("12000.00"));

        when(payslipRepository.findByIdAndActiveTrue(9001L)).thenReturn(Optional.of(payslip));
        when(payslipLineItemRepository.findByPayslip_IdAndActiveTrueOrderByIdAsc(9001L)).thenReturn(List.of(item));

        PayslipDetailDTO result = service.getPayslipById(9001L);
        assertEquals(1, result.lineItems().size());
        assertEquals("BASIC", result.lineItems().get(0).componentCode());
    }

    @Test
    void createRunAppliesLopDeductionInTotals() {
        Staff staff = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        StaffSalaryMapping mapping = buildMapping(1001L, staff);

        when(payrollRunRepository.existsByPayYearAndPayMonthAndActiveTrue(2026, 4)).thenReturn(false);
        when(staffSalaryMappingRepository.findActiveMappingsEffectiveInRange(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(9999, 12, 31)
        )).thenReturn(List.of(mapping));
        when(academicCalendarEventRepository.findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(
                "2026-2027",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of());
        when(leaveApplicationRepository.sumApprovedLopDaysOverlapping(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(new BigDecimal("1.00"));
        when(staffDailyAttendanceRepository.countByStaffIdAndAttendanceDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(0L);
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(invocation -> {
            PayrollRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(500L);
            }
            if (run.getProcessedOn() == null) {
                run.setProcessedOn(LocalDateTime.now());
            }
            run.setStatus(PayrollRunStatus.PROCESSED);
            return run;
        });

        when(staffSalaryMappingService.computeBreakdown(1001L)).thenReturn(new ComputedSalaryBreakdownDTO(
                101L,
                "Ravi Kumar",
                "EMP001",
                "Template A",
                "PRT",
                List.of(),
                List.of(),
                new BigDecimal("30000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("28500.00"),
                new BigDecimal("30000.00"),
                false
        ));
        when(payrollEntryRepository.save(any(PayrollEntry.class))).thenAnswer(invocation -> {
            PayrollEntry entry = invocation.getArgument(0);
            entry.setId(7002L);
            return entry;
        });
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(invocation -> {
            Payslip payslip = invocation.getArgument(0);
            if (payslip.getId() == null) {
                payslip.setId(9002L);
            }
            return payslip;
        });

        PayrollRunResponseDTO response = service.createRun(new PayrollRunCreateDTO(2026, 4, "April payroll"));

        assertEquals(new BigDecimal("2863.64"), response.totalDeductions());
        assertEquals(new BigDecimal("27136.36"), response.totalNet());
    }

    @Test
    void createRunReconcilesAttendanceMarksWithLopFloor() {
        Staff staff = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        StaffSalaryMapping mapping = buildMapping(1001L, staff);

        when(payrollRunRepository.existsByPayYearAndPayMonthAndActiveTrue(2026, 4)).thenReturn(false);
        when(staffSalaryMappingRepository.findActiveMappingsEffectiveInRange(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(9999, 12, 31)
        )).thenReturn(List.of(mapping));
        when(academicCalendarEventRepository.findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(
                "2026-2027",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of());
        when(leaveApplicationRepository.sumApprovedLopDaysOverlapping(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(new BigDecimal("3.20"));
        when(staffDailyAttendanceRepository.countByStaffIdAndAttendanceDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(21L);
        when(staffDailyAttendanceRepository.countPresentByStaffIdAndDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(20L);
        when(staffDailyAttendanceRepository.countAbsentByStaffIdAndDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(1L);

        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(invocation -> {
            PayrollRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(500L);
            }
            if (run.getProcessedOn() == null) {
                run.setProcessedOn(LocalDateTime.now());
            }
            run.setStatus(PayrollRunStatus.PROCESSED);
            return run;
        });
        when(staffSalaryMappingService.computeBreakdown(1001L)).thenReturn(new ComputedSalaryBreakdownDTO(
                101L,
                "Ravi Kumar",
                "EMP001",
                "Template A",
                "PRT",
                List.of(),
                List.of(),
                new BigDecimal("30000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("28500.00"),
                new BigDecimal("30000.00"),
                false
        ));
        when(payrollEntryRepository.save(any(PayrollEntry.class))).thenAnswer(invocation -> {
            PayrollEntry entry = invocation.getArgument(0);
            entry.setId(7003L);
            return entry;
        });

        AtomicReference<Payslip> savedPayslipRef = new AtomicReference<>();
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(invocation -> {
            Payslip payslip = invocation.getArgument(0);
            savedPayslipRef.set(payslip);
            if (payslip.getId() == null) {
                payslip.setId(9003L);
            }
            return payslip;
        });

        service.createRun(new PayrollRunCreateDTO(2026, 4, "April payroll"));

        Payslip savedPayslip = savedPayslipRef.get();
        int expectedAbsentDays = Math.max(savedPayslip.getTotalWorkingDays() - 20, 4);
        int expectedPresentDays = Math.max(0, savedPayslip.getTotalWorkingDays() - expectedAbsentDays);
        assertEquals(expectedAbsentDays, savedPayslip.getDaysAbsent());
        assertEquals(expectedPresentDays, savedPayslip.getDaysPresent());
    }

    @Test
    void createRunHonorsMarkedOnlyPolicyForPartialAttendance() {
        ReflectionTestUtils.setField(service, "partialMarkPolicy", "MARKED_ONLY");

        Staff staff = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        StaffSalaryMapping mapping = buildMapping(1001L, staff);

        when(payrollRunRepository.existsByPayYearAndPayMonthAndActiveTrue(2026, 4)).thenReturn(false);
        when(staffSalaryMappingRepository.findActiveMappingsEffectiveInRange(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(9999, 12, 31)
        )).thenReturn(List.of(mapping));
        when(academicCalendarEventRepository.findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(
                "2026-2027",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of());
        when(leaveApplicationRepository.sumApprovedLopDaysOverlapping(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(BigDecimal.ZERO);
        when(staffDailyAttendanceRepository.countByStaffIdAndAttendanceDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(5L);
        when(staffDailyAttendanceRepository.countPresentByStaffIdAndDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(5L);
        when(staffDailyAttendanceRepository.countAbsentByStaffIdAndDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(0L);

        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(invocation -> {
            PayrollRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(500L);
            }
            if (run.getProcessedOn() == null) {
                run.setProcessedOn(LocalDateTime.now());
            }
            run.setStatus(PayrollRunStatus.PROCESSED);
            return run;
        });
        when(staffSalaryMappingService.computeBreakdown(1001L)).thenReturn(new ComputedSalaryBreakdownDTO(
                101L,
                "Ravi Kumar",
                "EMP001",
                "Template A",
                "PRT",
                List.of(),
                List.of(),
                new BigDecimal("30000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("28500.00"),
                new BigDecimal("30000.00"),
                false
        ));
        when(payrollEntryRepository.save(any(PayrollEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicReference<Payslip> savedPayslipRef = new AtomicReference<>();
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(invocation -> {
            Payslip payslip = invocation.getArgument(0);
            savedPayslipRef.set(payslip);
            return payslip;
        });

        service.createRun(new PayrollRunCreateDTO(2026, 4, "April payroll"));

        Payslip savedPayslip = savedPayslipRef.get();
        assertEquals(5, savedPayslip.getDaysPresent());
        assertEquals(0, savedPayslip.getDaysAbsent());
    }

    @Test
    void createRunFailsOnPartialAttendanceWhenPolicyIsStrict() {
        ReflectionTestUtils.setField(service, "partialMarkPolicy", "FAIL_ON_PARTIAL");

        Staff staff = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        StaffSalaryMapping mapping = buildMapping(1001L, staff);

        when(payrollRunRepository.existsByPayYearAndPayMonthAndActiveTrue(2026, 4)).thenReturn(false);
        when(staffSalaryMappingRepository.findActiveMappingsEffectiveInRange(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(9999, 12, 31)
        )).thenReturn(List.of(mapping));
        when(academicCalendarEventRepository.findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(
                "2026-2027",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of());
        when(leaveApplicationRepository.sumApprovedLopDaysOverlapping(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(BigDecimal.ZERO);
        when(staffDailyAttendanceRepository.countByStaffIdAndAttendanceDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(5L);
        when(staffDailyAttendanceRepository.countPresentByStaffIdAndDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(5L);
        when(staffDailyAttendanceRepository.countAbsentByStaffIdAndDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(0L);

        when(staffSalaryMappingService.computeBreakdown(1001L)).thenReturn(new ComputedSalaryBreakdownDTO(
                101L,
                "Ravi Kumar",
                "EMP001",
                "Template A",
                "PRT",
                List.of(),
                List.of(),
                new BigDecimal("30000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("28500.00"),
                new BigDecimal("30000.00"),
                false
        ));

        assertThrows(EdusyncException.class, () -> service.createRun(new PayrollRunCreateDTO(2026, 4, "April payroll")));
    }

    @Test
    void getPayslipPdfReturnsBytes() {
        PayrollRun run = new PayrollRun();
        run.setId(500L);

        Staff staff = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        Payslip payslip = new Payslip();
        payslip.setId(9001L);
        payslip.setPayrollRun(run);
        payslip.setStaff(staff);
        payslip.setPayMonth(4);
        payslip.setPayYear(2026);
        payslip.setTotalWorkingDays(26);
        payslip.setDaysPresent(25);
        payslip.setDaysAbsent(1);
        payslip.setLopDays(new BigDecimal("1.00"));
        payslip.setGrossPay(new BigDecimal("16800.00"));
        payslip.setTotalDeductions(new BigDecimal("1440.00"));
        payslip.setNetPay(new BigDecimal("15360.00"));
        payslip.setStatus(PayrollRunStatus.PROCESSED);
        payslip.setGeneratedAt(LocalDateTime.now());

        when(payslipRepository.findByIdAndActiveTrue(9001L)).thenReturn(Optional.of(payslip));
        when(payslipLineItemRepository.findByPayslip_IdAndActiveTrueOrderByIdAsc(9001L)).thenReturn(List.of());
        when(pdfGenerationService.generatePdfFromHtml(any(), any())).thenReturn(new byte[]{7, 8, 9});

        byte[] result = service.getPayslipPdf(9001L);
        assertEquals(3, result.length);
    }

    @Test
    void listMyPayslipsReturnsOnlyCurrentStaffData() {
        Staff me = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        Payslip payslip = new Payslip();
        payslip.setId(9001L);
        payslip.setStaff(me);
        payslip.setPayrollRun(new PayrollRun());
        payslip.setPayMonth(4);
        payslip.setPayYear(2026);
        payslip.setGrossPay(new BigDecimal("100.00"));
        payslip.setTotalDeductions(new BigDecimal("10.00"));
        payslip.setNetPay(new BigDecimal("90.00"));
        payslip.setStatus(PayrollRunStatus.PROCESSED);
        payslip.setGeneratedAt(LocalDateTime.now());

        when(authUtil.getCurrentUserId()).thenReturn(2001L);
        when(staffRepository.findByUserProfile_User_Id(2001L)).thenReturn(Optional.of(me));
        when(payslipRepository.findByStaff_IdAndActiveTrueOrderByPayYearDescPayMonthDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(payslip), PageRequest.of(0, 20), 1));

        org.springframework.data.domain.Page<PayslipSummaryDTO> result = service.listMyPayslips(PageRequest.of(0, 20));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMyPayslipByIdThrowsForbiddenForOtherStaffPayslip() {
        Staff me = buildStaff(101L, "EMP001", "Ravi", "Kumar");
        Staff other = buildStaff(102L, "EMP002", "Asha", "Verma");

        Payslip payslip = new Payslip();
        payslip.setId(9002L);
        payslip.setStaff(other);
        payslip.setPayrollRun(new PayrollRun());
        payslip.setStatus(PayrollRunStatus.PROCESSED);

        when(authUtil.getCurrentUserId()).thenReturn(2001L);
        when(staffRepository.findByUserProfile_User_Id(2001L)).thenReturn(Optional.of(me));
        when(payslipRepository.findByIdAndActiveTrue(9002L)).thenReturn(Optional.of(payslip));

        assertThrows(EdusyncException.class, () -> service.getMyPayslipById(9002L));
    }

    private StaffSalaryMapping buildMapping(Long mappingId, Staff staff) {
        StaffSalaryMapping mapping = new StaffSalaryMapping();
        mapping.setId(mappingId);
        mapping.setStaff(staff);
        mapping.setActive(true);
        mapping.setRemarks("Mapping");
        return mapping;
    }

    private Staff buildStaff(Long staffId, String employeeId, String firstName, String lastName) {
        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);

        Staff staff = new Staff();
        staff.setId(staffId);
        staff.setEmployeeId(employeeId);
        staff.setUserProfile(profile);
        staff.setActive(true);
        return staff;
    }
}















