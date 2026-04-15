package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.settings.service.AppSettingService;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.hrms.dto.loan.LoanDTOs.*;
import com.project.edusync.hrms.model.entity.*;
import com.project.edusync.hrms.model.enums.LoanStatus;
import com.project.edusync.hrms.model.enums.RepaymentStatus;
import com.project.edusync.hrms.repository.*;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service("loanService")
@RequiredArgsConstructor
public class LoanServiceImpl {

    private final StaffLoanRepository loanRepo;
    private final LoanRepaymentRecordRepository repaymentRepo;
    private final StaffRepository staffRepository;
    private final PdfGenerationService pdfGenerationService;
    private final AppSettingService appSettingService;

    @Transactional
    public LoanResponseDTO applyLoan(LoanApplicationDTO dto) {
        Staff staff = resolveStaff(dto.staffRef());
        StaffLoan loan = new StaffLoan();
        loan.setStaff(staff);
        loan.setLoanType(dto.loanType());
        loan.setPrincipalAmount(dto.principalAmount());
        loan.setEmiCount(dto.emiCount());
        loan.setInterestRate(dto.interestRate() != null ? dto.interestRate() : BigDecimal.ZERO);
        loan.setStatus(LoanStatus.PENDING);
        loan.setRemarks(dto.reason());
        return toLoanResponse(loanRepo.save(loan));
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> listLoans(String staffRef, LoanStatus status) {
        if (staffRef != null) {
            Staff staff = resolveStaff(staffRef);
            List<StaffLoan> loans = status != null
                    ? loanRepo.findByStaff_IdAndStatus(staff.getId(), status)
                    : loanRepo.findByStaff_Id(staff.getId());
            return loans.stream().map(this::toLoanResponse).toList();
        }
        List<StaffLoan> loans = status != null
                ? loanRepo.findByStatusIn(List.of(status))
                : loanRepo.findAll();
        return loans.stream().map(this::toLoanResponse).toList();
    }

    @Transactional(readOnly = true)
    public LoanResponseDTO getLoan(UUID uuid) {
        return toLoanResponse(findLoan(uuid));
    }

    @Transactional
    public LoanResponseDTO updateLoanStatus(UUID uuid, LoanStatusUpdateDTO dto) {
        StaffLoan loan = findLoan(uuid);
        loan.setStatus(dto.status());
        if (dto.approvedAmount() != null) loan.setApprovedAmount(dto.approvedAmount());
        if (dto.disbursedAt() != null) loan.setDisbursedAt(dto.disbursedAt());
        if (dto.emiAmount() != null) loan.setEmiAmount(dto.emiAmount());
        if (dto.emiCount() != null) {
            loan.setEmiCount(dto.emiCount());
            loan.setRemainingEmis(dto.emiCount());
        }
        if (dto.remarks() != null) loan.setRemarks(dto.remarks());
        if (dto.status() == LoanStatus.DISBURSED) scheduleRepayments(loan);
        return toLoanResponse(loanRepo.save(loan));
    }

    @Transactional(readOnly = true)
    public List<RepaymentDTO> listRepayments(UUID loanUuid) {
        StaffLoan loan = findLoan(loanUuid);
        return repaymentRepo.findByLoan_Id(loan.getId()).stream().map(this::toRepaymentDTO).toList();
    }

    @Transactional
    public RepaymentDTO manualRepayment(UUID loanUuid, ManualRepaymentDTO dto) {
        StaffLoan loan = findLoan(loanUuid);
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.DISBURSED)
            throw new EdusyncException("Loan is not active for repayment", HttpStatus.BAD_REQUEST);
        LoanRepaymentRecord rep = new LoanRepaymentRecord();
        rep.setLoan(loan);
        rep.setAmount(dto.amount());
        rep.setPaidDate(dto.paidDate() != null ? dto.paidDate() : LocalDate.now());
        rep.setStatus(RepaymentStatus.MANUAL);
        rep.setRemarks(dto.remarks());
        if (loan.getRemainingEmis() != null && loan.getRemainingEmis() > 0) {
            loan.setRemainingEmis(loan.getRemainingEmis() - 1);
            if (loan.getRemainingEmis() == 0) loan.setStatus(LoanStatus.CLOSED);
            loanRepo.save(loan);
        }
        return toRepaymentDTO(repaymentRepo.save(rep));
    }

    @Transactional(readOnly = true)
    public LoanSummaryDTO getLoanSummary(String staffRef) {
        Staff staff = resolveStaff(staffRef);
        List<StaffLoan> loans = loanRepo.findByStaff_IdAndStatusIn(staff.getId(),
                List.of(LoanStatus.ACTIVE, LoanStatus.DISBURSED));
        BigDecimal totalPrincipal = loans.stream().map(l -> l.getApprovedAmount() != null ? l.getApprovedAmount() : l.getPrincipalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRepaid = loans.stream().flatMap(l -> repaymentRepo.findByLoan_Id(l.getId()).stream())
                .filter(r -> r.getStatus() == RepaymentStatus.DEDUCTED || r.getStatus() == RepaymentStatus.MANUAL)
                .map(LoanRepaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = totalPrincipal.subtract(totalRepaid);
        String name = staff.getUserProfile() != null ? (staff.getUserProfile().getFirstName() + " " + staff.getUserProfile().getLastName()).trim() : "";
        return new LoanSummaryDTO(staff.getUuid(), name, totalPrincipal, totalRepaid, outstanding, loans.size(), null, null);
    }

    @Transactional(readOnly = true)
    public byte[] getLoanDocumentPdf(UUID loanUuid) {
        StaffLoan loan = findLoan(loanUuid);
        List<RepaymentDTO> repayments = listRepayments(loanUuid);

        BigDecimal totalRepayable = repayments.stream()
                .map(RepaymentDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount = repayments.stream()
                .filter(r -> r.status() == RepaymentStatus.DEDUCTED || r.status() == RepaymentStatus.MANUAL)
                .count();

        Map<String, Object> data = new HashMap<>();
        data.put("loan", toLoanResponse(loan));
        data.put("repayments", repayments);
        data.put("totalRepayable", totalRepayable);
        data.put("paidCount", paidCount);
        data.put("generatedAt", LocalDateTime.now());
        data.put("schoolName", appSettingService.getValue("school.name", "Institution"));
        data.put("schoolAddress", appSettingService.getValue("school.address", ""));
        data.put("schoolPhone", appSettingService.getValue("school.phone", ""));

        return pdfGenerationService.generatePdfFromHtml("hrms/loan-sanction-letter", data);
    }

    private void scheduleRepayments(StaffLoan loan) {
        if (loan.getEmiCount() == null || loan.getDisbursedAt() == null) return;
        // If emiAmount not already set, compute via PMT formula
        if (loan.getEmiAmount() == null || loan.getEmiAmount().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal principal = loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getPrincipalAmount();
            BigDecimal annualRate = loan.getInterestRate();
            BigDecimal emi;
            if (annualRate != null && annualRate.compareTo(BigDecimal.ZERO) > 0) {
                // PMT formula: EMI = P * r * (1+r)^n / ((1+r)^n - 1), r = monthly rate
                double r = annualRate.doubleValue() / 100.0 / 12.0;
                int n = loan.getEmiCount();
                double pow = Math.pow(1 + r, n);
                double emiD = principal.doubleValue() * r * pow / (pow - 1);
                emi = BigDecimal.valueOf(emiD).setScale(2, RoundingMode.HALF_UP);
            } else {
                // Zero-interest: simple division
                emi = principal.divide(BigDecimal.valueOf(loan.getEmiCount()), 2, RoundingMode.HALF_UP);
            }
            loan.setEmiAmount(emi);
        }
        for (int i = 1; i <= loan.getEmiCount(); i++) {
            LoanRepaymentRecord r = new LoanRepaymentRecord();
            r.setLoan(loan);
            r.setDueDate(loan.getDisbursedAt().plusMonths(i));
            r.setAmount(loan.getEmiAmount());
            r.setStatus(RepaymentStatus.SCHEDULED);
            repaymentRepo.save(r);
        }
        loan.setRemainingEmis(loan.getEmiCount());
        loan.setStatus(LoanStatus.ACTIVE);
    }

    private StaffLoan findLoan(UUID uuid) {
        return loanRepo.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + uuid));
    }

    private Staff resolveStaff(String ref) {
        return PublicIdentifierResolver.resolve(ref, staffRepository::findByUuid, staffRepository::findById, "Staff");
    }

    private String name(Staff s) {
        return s.getUserProfile() != null ? (s.getUserProfile().getFirstName() + " " + s.getUserProfile().getLastName()).trim() : "";
    }

    private LoanResponseDTO toLoanResponse(StaffLoan l) {
        return new LoanResponseDTO(l.getUuid(), l.getStaff().getUuid(), name(l.getStaff()), l.getLoanType(),
                l.getPrincipalAmount(), l.getApprovedAmount(), l.getEmiAmount(), l.getEmiCount(),
                l.getRemainingEmis(), l.getInterestRate(), l.getStatus(), l.getDisbursedAt(),
                l.getRemarks(), l.getCreatedAt());
    }

    private RepaymentDTO toRepaymentDTO(LoanRepaymentRecord r) {
        return new RepaymentDTO(r.getId(), r.getUuid(), r.getDueDate(), r.getAmount(),
                r.getPaidDate(), r.getPayrollRunRef(), r.getStatus());
    }
}

