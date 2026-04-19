package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.designation.StaffDesignationResponseDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveBalanceResponseDTO;
import com.project.edusync.hrms.dto.payroll.PayslipSummaryDTO;
import com.project.edusync.hrms.dto.promotion.PromotionResponseDTO;
import com.project.edusync.hrms.dto.payroll.StaffAttendanceSummaryDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.dto.loan.LoanDTOs.LoanResponseDTO;
import com.project.edusync.hrms.dto.overtime.OvertimeDTOs.OvertimeResponseDTO;
import com.project.edusync.hrms.dto.staff360.Staff360ProfileDTO;
import com.project.edusync.hrms.model.entity.*;
import com.project.edusync.hrms.model.enums.PromotionStatus;
import com.project.edusync.hrms.repository.*;
import com.project.edusync.ams.model.entity.StaffDailyAttendance;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.hrms.service.StaffSalaryMappingService;
import com.project.edusync.uis.model.dto.admin.StaffSummaryDTO;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service("staff360Service")
@RequiredArgsConstructor
public class Staff360ServiceImpl {

    private final StaffRepository staffRepository;
    private final StaffDocumentRepository documentRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PayslipRepository payslipRepository;
    private final PromotionRequestRepository promotionRequestRepository;
    private final StaffGradeAssignmentRepository gradeAssignmentRepository;
    private final StaffDailyAttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final StaffSalaryMappingRepository staffSalaryMappingRepository;
    private final StaffSalaryMappingService staffSalaryMappingService;
    private final StaffLoanRepository staffLoanRepository;
    private final OvertimeRecordRepository overtimeRecordRepository;

    @Transactional(readOnly = true)
    public Staff360ProfileDTO getProfile(String staffRef) {
        Staff staff = PublicIdentifierResolver.resolve(staffRef,
                staffRepository::findByUuid, staffRepository::findById, "Staff");

        // 1. Personal / StaffSummaryDTO
        StaffSummaryDTO personal = StaffSummaryDTO.builder()
                .staffId(staff.getId())
                .uuid(staff.getUuid().toString())
                .employeeId(staff.getEmployeeId())
                .jobTitle(staff.getJobTitle())
                .department(null)
                .category(staff.getCategory())
                .staffType(staff.getStaffType())
                .hireDate(staff.getHireDate())
                .active(staff.isActive())
                .build();

        if (staff.getUserProfile() != null) {
            personal.setFirstName(staff.getUserProfile().getFirstName());
            personal.setMiddleName(staff.getUserProfile().getMiddleName());
            personal.setLastName(staff.getUserProfile().getLastName());
            personal.setProfileUrl(staff.getUserProfile().getProfileUrl());
            personal.setGender(staff.getUserProfile().getGender() != null ? staff.getUserProfile().getGender().name() : null);
            personal.setDateOfBirth(staff.getUserProfile().getDateOfBirth());
            if (staff.getUserProfile().getUser() != null) {
                personal.setEmail(staff.getUserProfile().getUser().getUsername());
            }
        }

        // 2. Designation
        StaffDesignationResponseDTO currentDesignation = null;
        if (staff.getDesignation() != null) {
            var d = staff.getDesignation();
            currentDesignation = new StaffDesignationResponseDTO(
                    d.getId(),
                    d.getUuid().toString(),
                    d.getDesignationCode(),
                    d.getDesignationName(),
                    staff.getCategory(),
                    d.getDescription(),
                    d.getSortOrder(),
                    d.isActive(),
                    null, null, null, null, null,
                    d.getCreatedAt(),
                    d.getUpdatedAt()
            );
            personal.setDesignationName(d.getDesignationName());
            personal.setDesignationCode(d.getDesignationCode());
        }

        // 3. Grade
        StaffGradeAssignmentResponseDTO currentGrade = null;
        var gradeAssignmentOpt = gradeAssignmentRepository.findByStaff_IdAndActiveTrueAndEffectiveToIsNull(staff.getId());
        if (gradeAssignmentOpt.isPresent()) {
            var ga = gradeAssignmentOpt.get();
            currentGrade = new StaffGradeAssignmentResponseDTO(
                    ga.getId(),
                    ga.getUuid().toString(),
                    staff.getId(),
                    personal.getFirstName(),
                    ga.getGrade() != null ? ga.getGrade().getId() : null,
                    ga.getGrade() != null ? ga.getGrade().getGradeCode() : null,
                    ga.getGrade() != null ? ga.getGrade().getGradeName() : null,
                    ga.getEffectiveFrom(),
                    ga.getEffectiveTo(),
                    ga.getPromotionOrderRef(),
                    ga.getPromotedBy() != null ? ga.getPromotedBy().getId() : null,
                    ga.getRemarks(),
                    ga.getCreatedAt()
            );
        }

        // 4. Salary breakdown
        ComputedSalaryBreakdownDTO salaryStructure = null;
        var mappings = staffSalaryMappingRepository.findActiveMappingsEffectiveInRange(LocalDate.now(), LocalDate.now(), LocalDate.now());
        var activeMapping = mappings.stream().filter(m -> m.getStaff().getId().equals(staff.getId())).findFirst();
        if (activeMapping.isPresent()) {
            salaryStructure = staffSalaryMappingService.computeBreakdown(activeMapping.get().getId());
        }

        // 5. Leave Balances
        int startYear = LocalDate.now().getMonthValue() >= 4 ? LocalDate.now().getYear() : LocalDate.now().getYear() - 1;
        String academicYear = startYear + "-" + (startYear + 1);
        List<LeaveBalanceResponseDTO> leaveBalances = leaveBalanceRepository
                .findByStaff_IdAndAcademicYearAndActiveTrueOrderByLeaveType_LeaveCodeAsc(staff.getId(), academicYear)
                .stream().map(lb -> new LeaveBalanceResponseDTO(
                        lb.getId(),
                        staff.getId(),
                        personal.getFirstName(),
                        lb.getLeaveType() != null ? lb.getLeaveType().getId() : null,
                        lb.getLeaveType() != null ? lb.getLeaveType().getLeaveCode() : null,
                        lb.getLeaveType() != null ? lb.getLeaveType().getDisplayName() : null,
                        lb.getAcademicYear(),
                        lb.getTotalQuota(),
                        lb.getUsed(),
                        BigDecimal.ZERO,
                        lb.getTotalQuota().subtract(lb.getUsed())
                )).toList();

        // 6. Attendance Summary (last 30 days heatmap)
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        List<StaffDailyAttendance> attendanceRecords = attendanceRepository.findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(staff.getId(), thirtyDaysAgo, today);
        
        int presentCount = (int) attendanceRecords.stream().filter(r -> r.getAttendanceType() != null && r.getAttendanceType().isPresentMark()).count();
        int absentCount = (int) attendanceRecords.stream().filter(r -> r.getAttendanceType() != null && r.getAttendanceType().isAbsenceMark()).count();
        int onLeaveCount = leaveApplicationRepository.findApprovedActiveByStaffIdAndDateRange(staff.getId(), thirtyDaysAgo, today).size();
        int workingDays = presentCount + absentCount;
        double attendancePct = workingDays > 0 ? (double) presentCount / workingDays * 100 : 0;

        List<StaffAttendanceSummaryDTO.DayRecord> dailyRecords = attendanceRecords.stream()
                .map(r -> new StaffAttendanceSummaryDTO.DayRecord(
                        r.getAttendanceDate(), 
                        r.getAttendanceType() != null ? r.getAttendanceType().getShortCode() : "UNMARKED"))
                .toList();

        StaffAttendanceSummaryDTO attendanceSummary = StaffAttendanceSummaryDTO.builder()
                .periodLabel("Last 30 Days")
                .totalDays(30)
                .presentDays(presentCount)
                .absentDays(absentCount)
                .leaveDays(onLeaveCount)
                .attendancePercentage(BigDecimal.valueOf(attendancePct).setScale(1, RoundingMode.HALF_UP))
                .dailyRecords(dailyRecords)
                .build();

        // 7. Recent Payslips
        List<PayslipSummaryDTO> recentPayslips = payslipRepository.findByStaff_IdAndActiveTrueOrderByPayYearDescPayMonthDesc(staff.getId(), PageRequest.of(0, 5))
                .stream().map(p -> new PayslipSummaryDTO(
                        p.getId(),
                        p.getUuid().toString(),
                        p.getPayrollRun() != null ? p.getPayrollRun().getId() : null,
                        staff.getId(),
                        personal.getFirstName(),
                        staff.getEmployeeId(),
                        p.getPayMonth(),
                        p.getPayYear(),
                        p.getGrossPay(),
                        p.getTotalDeductions(),
                        p.getNetPay(),
                        p.getStatus(),
                        p.getGeneratedAt()
                )).toList();

        // 8. Promotion History
        List<PromotionResponseDTO> promotions = promotionRequestRepository.findByStaff_IdAndActiveTrueOrderByEffectiveDateDesc(staff.getId())
                .stream().map(p -> new PromotionResponseDTO(
                        p.getId(),
                        p.getUuid().toString(),
                        staff.getId(),
                        personal.getFirstName(),
                        staff.getEmployeeId(),
                        p.getCurrentDesignation() != null ? p.getCurrentDesignation().getId() : null,
                        p.getCurrentDesignation() != null ? p.getCurrentDesignation().getDesignationCode() : null,
                        p.getCurrentDesignation() != null ? p.getCurrentDesignation().getDesignationName() : null,
                        p.getProposedDesignation() != null ? p.getProposedDesignation().getId() : null,
                        p.getProposedDesignation() != null ? p.getProposedDesignation().getDesignationCode() : null,
                        p.getProposedDesignation() != null ? p.getProposedDesignation().getDesignationName() : null,
                        p.getCurrentGrade() != null ? p.getCurrentGrade().getId() : null,
                        p.getCurrentGrade() != null ? p.getCurrentGrade().getGradeCode() : null,
                        p.getProposedGrade() != null ? p.getProposedGrade().getId() : null,
                        p.getProposedGrade() != null ? p.getProposedGrade().getGradeCode() : null,
                        p.getNewSalaryTemplate() != null ? p.getNewSalaryTemplate().getId() : null,
                        p.getNewSalaryTemplate() != null ? p.getNewSalaryTemplate().getTemplateName() : null,
                        p.getEffectiveDate(),
                        p.getStatus(),
                        p.getInitiatedByUserId(),
                        p.getApprovedByUserId(),
                        p.getApprovedByName(),
                        p.getApprovedOn(),
                        p.getOrderReference(),
                        p.getRemarks(),
                        p.getCreatedAt(),
                        p.getUpdatedAt()
                )).toList();

        int docCount = documentRepository.findByStaff_IdAndActiveTrue(staff.getId()).size();

        // 9. Loans - all time
        List<LoanResponseDTO> loans = staffLoanRepository.findByStaff_Id(staff.getId())
                .stream().map(l -> new LoanResponseDTO(
                        l.getUuid(),
                        staff.getUuid(),
                        personal.getFirstName() + (personal.getLastName() != null ? " " + personal.getLastName() : ""),
                        l.getLoanType(),
                        l.getPrincipalAmount(),
                        l.getApprovedAmount(),
                        l.getEmiAmount(),
                        l.getEmiCount(),
                        l.getRemainingEmis(),
                        l.getInterestRate(),
                        l.getStatus(),
                        l.getDisbursedAt(),
                        l.getRemarks(),
                        l.getCreatedAt()
                )).toList();

        int activeLoans = (int) loans.stream().filter(l -> l.status() != null &&
                (l.status().name().equals("ACTIVE") || l.status().name().equals("APPROVED"))).count();

        // 10. Overtime - last 12 months
        LocalDate overtimeFrom = today.minusMonths(12);
        List<OvertimeResponseDTO> overtimes = overtimeRecordRepository.findByStaff_Id(staff.getId())
                .stream()
                .filter(o -> o.getWorkDate() != null && !o.getWorkDate().isBefore(overtimeFrom))
                .map(o -> new OvertimeResponseDTO(
                        o.getUuid(),
                        staff.getUuid(),
                        personal.getFirstName() + (personal.getLastName() != null ? " " + personal.getLastName() : ""),
                        o.getWorkDate(),
                        o.getHoursWorked(),
                        o.getReason(),
                        o.getStatus(),
                        o.getCompensationType(),
                        o.getApprovedAt(),
                        o.getMultiplier(),
                        o.getApprovedAmount(),
                        o.getPayrollRunRef()
                )).toList();

        return new Staff360ProfileDTO(
                personal,
                currentGrade,
                currentDesignation,
                salaryStructure,
                leaveBalances,
                attendanceSummary,
                recentPayslips,
                promotions,
                loans,
                overtimes,
                docCount,
                activeLoans,
                "COMPLETED"
        );
    }
}
