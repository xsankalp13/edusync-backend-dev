package com.project.edusync.hrms.dto.staff360;

import com.project.edusync.hrms.dto.designation.StaffDesignationResponseDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveBalanceResponseDTO;
import com.project.edusync.hrms.dto.payroll.PayslipSummaryDTO;
import com.project.edusync.hrms.dto.payroll.StaffAttendanceSummaryDTO;
import com.project.edusync.hrms.dto.promotion.PromotionResponseDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.dto.loan.LoanDTOs.LoanResponseDTO;
import com.project.edusync.hrms.dto.overtime.OvertimeDTOs.OvertimeResponseDTO;
import com.project.edusync.uis.model.dto.admin.StaffSummaryDTO;

import java.util.List;

public record Staff360ProfileDTO(
        StaffSummaryDTO personal,
        StaffGradeAssignmentResponseDTO currentGrade,
        StaffDesignationResponseDTO currentDesignation,
        ComputedSalaryBreakdownDTO salaryStructure,
        List<LeaveBalanceResponseDTO> leaveBalance,
        StaffAttendanceSummaryDTO attendanceSummary,
        List<PayslipSummaryDTO> recentPayslips,
        List<PromotionResponseDTO> promotionHistory,
        List<LoanResponseDTO> loans,
        List<OvertimeResponseDTO> overtimes,
        int documentCount,
        int activeLoans,
        String onboardingStatus
) {
}
