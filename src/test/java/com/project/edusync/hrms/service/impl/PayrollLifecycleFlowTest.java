package com.project.edusync.hrms.service.impl;

import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.hrms.dto.payroll.PayrollRunResponseDTO;
import com.project.edusync.hrms.model.entity.PayrollRun;
import com.project.edusync.hrms.model.entity.Payslip;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.repository.PayslipLineItemRepository;
import com.project.edusync.hrms.repository.PayslipRepository;
import com.project.edusync.hrms.repository.PayrollEntryRepository;
import com.project.edusync.hrms.repository.PayrollRunRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
import com.project.edusync.hrms.service.StaffSalaryMappingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollLifecycleFlowTest {

    @Mock private PayrollRunRepository payrollRunRepository;
    @Mock private PayrollEntryRepository payrollEntryRepository;
    @Mock private PayslipRepository payslipRepository;
    @Mock private PayslipLineItemRepository payslipLineItemRepository;
    @Mock private AcademicCalendarEventRepository academicCalendarEventRepository;
    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private StaffSalaryMappingRepository staffSalaryMappingRepository;
    @Mock private StaffSalaryMappingService staffSalaryMappingService;
    @Mock private PdfGenerationService pdfGenerationService;
    @Mock private StaffDailyAttendanceRepository staffDailyAttendanceRepository;

    @InjectMocks
    private PayrollServiceImpl service;

    @Test
    void approveThenDisburseUpdatesRunAndPayslipStatuses() {
        PayrollRun run = new PayrollRun();
        run.setId(501L);
        run.setActive(true);
        run.setPayMonth(4);
        run.setPayYear(2026);
        run.setStatus(PayrollRunStatus.PROCESSED);
        run.setProcessedOn(LocalDateTime.now());
        run.setTotalStaff(0);

        Payslip payslip = new Payslip();
        payslip.setId(1001L);
        payslip.setStatus(PayrollRunStatus.PROCESSED);

        when(payrollRunRepository.findByIdAndActiveTrue(501L)).thenReturn(Optional.of(run));
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payrollEntryRepository.findByPayrollRun_IdAndActiveTrueOrderByStaff_IdAsc(501L)).thenReturn(List.of());
        when(payslipRepository.findByPayrollRun_IdAndActiveTrue(501L)).thenReturn(List.of(payslip));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        PayrollRunResponseDTO approved = service.approveRun(501L);
        assertEquals(PayrollRunStatus.APPROVED, approved.status());
        assertEquals(PayrollRunStatus.APPROVED, payslip.getStatus());

        PayrollRunResponseDTO disbursed = service.disburseRun(501L);
        assertEquals(PayrollRunStatus.DISBURSED, disbursed.status());
        assertEquals(PayrollRunStatus.DISBURSED, payslip.getStatus());
    }
}


