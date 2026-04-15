package com.project.edusync.hrms.dto.exit;

import com.project.edusync.hrms.model.enums.ClearanceItemType;
import com.project.edusync.hrms.model.enums.ExitRequestStatus;
import com.project.edusync.hrms.model.enums.FnFStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ExitDTOs {

    public record ExitRequestCreateDTO(@NotNull String staffRef, @NotNull LocalDate resignationDate,
                                        LocalDate lastWorkingDate, String exitReason) {}

    public record ExitRequestResponseDTO(UUID uuid, UUID staffRef, String staffName, LocalDate resignationDate,
                                          LocalDate lastWorkingDate, String exitReason,
                                          ExitRequestStatus status, LocalDateTime createdAt) {}

    public record ExitStatusUpdateDTO(@NotNull ExitRequestStatus status, String remarks) {}

    public record ClearanceItemCreateDTO(@NotNull ClearanceItemType itemType, String description,
                                          UUID responsiblePartyRef, String remarks) {}

    public record WaiveClearanceItemDTO(String waivedBy, String remarks) {}

    public record ClearanceItemResponseDTO(Long id, UUID uuid, ClearanceItemType itemType, String description,
                                            LocalDateTime completedAt, String completedByName,
                                            boolean waived, String waivedBy, LocalDateTime waivedAt,
                                            String remarks) {}

    public record FnFCreateDTO(BigDecimal grossSalaryDue, BigDecimal deductions, BigDecimal leaveEncashment,
                                BigDecimal gratuity, BigDecimal otherAdditions, BigDecimal otherDeductions,
                                String remarks) {}

    public record FnFResponseDTO(UUID uuid, UUID exitRequestRef, BigDecimal grossSalaryDue, BigDecimal deductions,
                                  BigDecimal leaveEncashment, BigDecimal gratuity, BigDecimal otherAdditions,
                                  BigDecimal otherDeductions, BigDecimal netPayable, FnFStatus status,
                                  LocalDateTime disbursedAt, String remarks) {}

    public record FnFStatusUpdateDTO(@NotNull FnFStatus status, String remarks) {}
}

