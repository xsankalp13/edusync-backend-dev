package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.payroll.PayrollRunCreateDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunResponseDTO;
import com.project.edusync.hrms.dto.payroll.PayrollRunSummaryDTO;
import com.project.edusync.hrms.dto.payroll.PayslipDetailDTO;
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
}





