package com.project.edusync.hrms.dto.expense;

import com.project.edusync.hrms.model.enums.ExpenseCategory;
import com.project.edusync.hrms.model.enums.ExpenseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ExpenseDTOs {

    public record ClaimCreateDTO(@NotBlank String staffRef, @NotBlank String title, String description) {}

    public record ClaimResponseDTO(UUID uuid, UUID staffRef, String staffName, String title,
                                    String description, BigDecimal totalAmount, ExpenseStatus status,
                                    LocalDateTime submittedAt, LocalDateTime createdAt,
                                    List<ClaimItemResponseDTO> items) {}

    public record ClaimStatusUpdateDTO(@NotNull ExpenseStatus status, String remarks) {}

    public record ClaimItemCreateDTO(@NotNull ExpenseCategory category, String description,
                                      @NotNull @Positive BigDecimal amount,
                                      String receiptUrl, @NotNull LocalDate expenseDate) {}

    public record ClaimItemUpdateDTO(ExpenseCategory category, String description,
                                      BigDecimal amount, String receiptUrl, LocalDate expenseDate) {}

    public record ClaimItemResponseDTO(Long id, UUID uuid, ExpenseCategory category, String description,
                                        BigDecimal amount, String receiptUrl, LocalDate expenseDate) {}

    public record ReceiptUploadInitRequestDTO(
            @NotBlank String fileName,
            @NotBlank String contentType,
            long sizeBytes
    ) {}
}

