package com.project.edusync.hrms.dto.overtime;

import com.project.edusync.hrms.model.enums.OvertimeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OvertimeDTOs {

    public record OvertimeCreateDTO(@NotBlank String staffRef, @NotNull LocalDate workDate,
                                     @NotNull @Positive BigDecimal hoursWorked, String reason,
                                     String compensationType) {}

    public record OvertimeApproveDTO(BigDecimal multiplier) {}

    public record OvertimeResponseDTO(UUID uuid, UUID staffRef, String staffName, LocalDate workDate,
                                       BigDecimal hoursWorked, String reason, OvertimeStatus status,
                                       String compensationType, LocalDateTime approvedAt,
                                       BigDecimal multiplier, BigDecimal approvedAmount, UUID payrollRunRef) {}

    public record CompOffCreateDTO(@NotBlank String staffRef, String overtimeRecordRef,
                                    String leaveTypeRef, @NotNull LocalDate creditDate,
                                    LocalDate expiryDate, String remarks) {}

    public record CompOffResponseDTO(UUID uuid, UUID staffRef, String staffName,
                                      UUID overtimeRecordRef, UUID leaveTypeRef, String leaveTypeName,
                                      LocalDate creditDate, LocalDate expiryDate,
                                      boolean credited, LocalDateTime creditedAt, String remarks) {}

    public record CompOffBalanceSummaryDTO(UUID staffRef, String staffName,
                                            List<AvailableCompOff> available) {
        public record AvailableCompOff(UUID compOffRef, LocalDate creditDate,
                                        LocalDate expiryDate, String leaveTypeName) {}
    }
}

