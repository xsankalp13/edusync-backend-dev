package com.project.edusync.hrms.service.impl;

import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.hrms.dto.payroll.PayrollRunCreateDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunResponseDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.model.entity.PayrollEntry;
import com.project.edusync.hrms.model.entity.PayrollRun;
import com.project.edusync.hrms.model.entity.Payslip;
import com.project.edusync.hrms.model.entity.StaffSalaryMapping;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollLeaveEndToEndFlowTest {

    @Mock private PayrollRunRepository payrollRunRepository;
    @Mock private PayrollEntryRepository payrollEntryRepository;
    @Mock private PayslipRepository payslipRepository;
    @Mock private PayslipLineItemRepository payslipLineItemRepository;
    @Mock private AcademicCalendarEventRepository academicCalendarEventRepository;
    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private StaffSalaryMappingRepository staffSalaryMappingRepository;
    @Mock private StaffSalaryMappingService staffSalaryMappingService;
    @Mock private PdfGenerationService pdfGenerationService;
    @Mock private AuthUtil authUtil;
    @Mock private StaffRepository staffRepository;
    @Mock private StaffDailyAttendanceRepository staffDailyAttendanceRepository;

    @InjectMocks
    private PayrollServiceImpl service;

    @Test
    void payrollRunUsesLopAndAttendanceMetricsAndSupportsLifecycleTransitions() {
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
        )).thenReturn(new BigDecimal("2.50"));
        when(staffDailyAttendanceRepository.countByStaffIdAndAttendanceDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(22L);
        when(staffDailyAttendanceRepository.countPresentByStaffIdAndDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(21L);
        when(staffDailyAttendanceRepository.countAbsentByStaffIdAndDateBetween(
                101L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(1L);

        when(staffSalaryMappingService.computeBreakdown(1001L)).thenReturn(new ComputedSalaryBreakdownDTO(
                101L,
                "Ravi Kumar",
                "EMP001",
                "Template A",
                "PRT",
                List.of(),
                List.of(),
                new BigDecimal("22000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("21000.00"),
                new BigDecimal("22000.00"),
                false
        ));

        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(invocation -> {
            PayrollRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(500L);
            }
            if (run.getProcessedOn() == null) {
                run.setProcessedOn(LocalDateTime.now());
            }
            return run;
        });
        when(payrollEntryRepository.save(any(PayrollEntry.class))).thenAnswer(invocation -> {
            PayrollEntry entry = invocation.getArgument(0);
            entry.setId(7001L);
            return entry;
        });

        AtomicReference<Payslip> savedPayslip = new AtomicReference<>();
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(invocation -> {
            Payslip payslip = invocation.getArgument(0);
            if (payslip.getId() == null) {
                payslip.setId(9001L);
            }
            savedPayslip.set(payslip);
            return payslip;
        });

        PayrollRunResponseDTO created = service.createRun(new PayrollRunCreateDTO(2026, 4, "April payroll"));

        assertEquals(PayrollRunStatus.PROCESSED, created.status());
        assertEquals(new BigDecimal("3500.00"), created.totalDeductions());
        assertEquals(new BigDecimal("18500.00"), created.totalNet());
        assertEquals(1, created.entries().size());

        Payslip payslip = savedPayslip.get();
        assertEquals(22, payslip.getTotalWorkingDays());
        assertEquals(19, payslip.getDaysPresent());
        assertEquals(3, payslip.getDaysAbsent());
        assertEquals(new BigDecimal("2.50"), payslip.getLopDays());

        when(payrollRunRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(buildRun(PayrollRunStatus.PROCESSED)));
        when(payrollEntryRepository.findByPayrollRun_IdAndActiveTrueOrderByStaff_IdAsc(500L)).thenReturn(List.of());
        when(payslipRepository.findByPayrollRun_IdAndActiveTrue(500L)).thenReturn(List.of(payslip));

        PayrollRunResponseDTO approved = service.approveRun(500L);
        assertEquals(PayrollRunStatus.APPROVED, approved.status());
        assertEquals(PayrollRunStatus.APPROVED, payslip.getStatus());

        when(payrollRunRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(buildRun(PayrollRunStatus.APPROVED)));
        PayrollRunResponseDTO disbursed = service.disburseRun(500L);
        assertEquals(PayrollRunStatus.DISBURSED, disbursed.status());
        assertEquals(PayrollRunStatus.DISBURSED, payslip.getStatus());
    }

    @Test
    void payrollRunTreatsUnmarkedWorkingDaysAsAbsentWhenAttendanceIsPartial() {
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
                new BigDecimal("20000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("19000.00"),
                new BigDecimal("20000.00"),
                false
        ));

        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(invocation -> {
            PayrollRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(501L);
            }
            if (run.getProcessedOn() == null) {
                run.setProcessedOn(LocalDateTime.now());
            }
            return run;
        });
        when(payrollEntryRepository.save(any(PayrollEntry.class))).thenAnswer(invocation -> {
            PayrollEntry entry = invocation.getArgument(0);
            entry.setId(7002L);
            return entry;
        });

        AtomicReference<Payslip> savedPayslip = new AtomicReference<>();
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(invocation -> {
            Payslip payslip = invocation.getArgument(0);
            if (payslip.getId() == null) {
                payslip.setId(9002L);
            }
            savedPayslip.set(payslip);
            return payslip;
        });

        service.createRun(new PayrollRunCreateDTO(2026, 4, "April payroll"));

        Payslip payslip = savedPayslip.get();
        assertEquals(22, payslip.getTotalWorkingDays());
        assertEquals(5, payslip.getDaysPresent());
        assertEquals(17, payslip.getDaysAbsent());
    }

    private PayrollRun buildRun(PayrollRunStatus status) {
        PayrollRun run = new PayrollRun();
        run.setId(500L);
        run.setActive(true);
        run.setPayYear(2026);
        run.setPayMonth(4);
        run.setProcessedOn(LocalDateTime.now());
        run.setStatus(status);
        run.setTotalStaff(1);
        run.setTotalGross(new BigDecimal("22000.00"));
        run.setTotalDeductions(new BigDecimal("3500.00"));
        run.setTotalNet(new BigDecimal("18500.00"));
        return run;
    }

    private StaffSalaryMapping buildMapping(Long mappingId, Staff staff) {
        StaffSalaryMapping mapping = new StaffSalaryMapping();
        mapping.setId(mappingId);
        mapping.setStaff(staff);
        mapping.setActive(true);
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


