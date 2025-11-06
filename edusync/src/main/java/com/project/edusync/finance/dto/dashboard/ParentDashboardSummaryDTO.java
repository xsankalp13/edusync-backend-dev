package com.project.edusync.finance.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

// DTO for GET /dashboard/summary (Parent)
@Data
@NoArgsConstructor
public class ParentDashboardSummaryDTO {

    private BigDecimal totalDue;      // SUM of (total - paid) for PENDING/OVERDUE invoices
    private LocalDate nextDueDate;   // MIN(dueDate) for PENDING/OVERDUE invoices
}