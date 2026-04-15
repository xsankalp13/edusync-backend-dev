package com.project.edusync.hrms.dto.loan;

import com.project.edusync.hrms.model.enums.LoanStatus;
import com.project.edusync.hrms.model.enums.RepaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class LoanDTOs {

    public record LoanApplicationDTO(@NotBlank String staffRef, @NotBlank String loanType,
                                      @NotNull @Positive BigDecimal principalAmount,
                                      Integer emiCount, BigDecimal interestRate, String reason) {}

    public record LoanResponseDTO(UUID uuid, UUID staffRef, String staffName, String loanType,
                                   BigDecimal principalAmount, BigDecimal approvedAmount,
                                   BigDecimal emiAmount, Integer emiCount, Integer remainingEmis,
                                   BigDecimal interestRate, LoanStatus status,
                                   LocalDate disbursedAt, String remarks, LocalDateTime createdAt) {}

    public record LoanStatusUpdateDTO(@NotNull LoanStatus status, BigDecimal approvedAmount,
                                       LocalDate disbursedAt, BigDecimal emiAmount,
                                       Integer emiCount, String remarks) {}

    public record RepaymentDTO(Long id, UUID uuid, LocalDate dueDate, BigDecimal amount,
                                LocalDate paidDate, UUID payrollRunRef, RepaymentStatus status) {}

    public record ManualRepaymentDTO(@NotNull @Positive BigDecimal amount,
                                      LocalDate paidDate, String remarks) {}

    public record LoanSummaryDTO(UUID staffRef, String staffName, BigDecimal totalPrincipal,
                                  BigDecimal totalRepaid, BigDecimal outstandingBalance,
                                  int activeLoans, LocalDate nextEmiDate, BigDecimal nextEmiAmount) {}
}

