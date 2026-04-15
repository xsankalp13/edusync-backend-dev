package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.payroll.BankSalaryAdviceDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunCreateDTO;
import com.project.edusync.hrms.dto.payroll.PayrollPreflightDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunResponseDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunSummaryDTO;
import com.project.edusync.hrms.dto.payroll.PayslipDetailDTO;
import com.project.edusync.hrms.dto.payroll.StaffAttendanceSummaryDTO;
import com.project.edusync.hrms.dto.payroll.PayslipSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PayrollService {

    PayrollRunResponseDTO createRun(PayrollRunCreateDTO dto);

    PayrollRunResponseDTO approveRun(Long runId);

    PayrollRunResponseDTO approveRunByIdentifier(String identifier);

    PayrollRunResponseDTO disburseRun(Long runId);

    PayrollRunResponseDTO disburseRunByIdentifier(String identifier);

    Page<PayslipSummaryDTO> listPayslipsByRun(Long runId, Pageable pageable);

    Page<PayslipSummaryDTO> listPayslipsByRunIdentifier(String identifier, Pageable pageable);

    PayslipDetailDTO getPayslipById(Long payslipId);

    PayslipDetailDTO getPayslipByIdentifier(String identifier);

    byte[] getPayslipPdf(Long payslipId);

    byte[] getPayslipPdfByIdentifier(String identifier);

    Page<PayslipSummaryDTO> listMyPayslips(Pageable pageable);

    PayslipDetailDTO getMyPayslipById(Long payslipId);

    PayslipDetailDTO getMyPayslipByIdentifier(String identifier);

    byte[] getMyPayslipPdf(Long payslipId);

    byte[] getMyPayslipPdfByIdentifier(String identifier);

    Page<PayslipSummaryDTO> listPayslipsByStaff(Long staffId, Pageable pageable);

    Page<PayslipSummaryDTO> listPayslipsByStaffIdentifier(String staffIdentifier, Pageable pageable);

    Page<PayrollRunSummaryDTO> listRuns(Pageable pageable);

    PayrollRunResponseDTO getRunById(Long runId);

    PayrollRunResponseDTO getRunByIdentifier(String identifier);

    StaffAttendanceSummaryDTO getMyAttendanceSummary(int year, int month);

    PayrollPreflightDTO getPayrollPreflight(int year, int month);

    /** Builds the Bank Salary Advice data model for the given payroll run. */
    BankSalaryAdviceDTO getBankSalaryAdvice(String runIdentifier);

    /** Generates the Bank Salary Advice as a PDF byte array (Thymeleaf → OpenHTMLtoPDF). */
    byte[] getBankSalaryAdvicePdf(String runIdentifier);

    /**
     * Bulk-marks all unmarked staff attendance as ABSENT for the given payroll period.
     * @return number of attendance records created
     */
    int markAllAbsentForPeriod(int year, int month);

    /**
     * Bulk-marks all unmarked staff attendance as PRESENT for the given payroll period.
     * @return number of attendance records created
     */
    int markAllPresentForPeriod(int year, int month);

    /**
     * Voids a PROCESSED (not yet DISBURSED) payroll run:
     * - Reverts loan repayment records back to SCHEDULED
     * - Reverts overtime records back to APPROVED
     * - Soft-deletes all payslips, line items, and payroll entries
     * - Marks the run as VOIDED and resets the duplicate-run guard
     */
    PayrollRunResponseDTO voidRun(String identifier);
}





