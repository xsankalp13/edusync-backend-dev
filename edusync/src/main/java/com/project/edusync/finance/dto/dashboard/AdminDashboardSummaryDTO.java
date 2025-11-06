package com.project.edusync.finance.dto.dashboard;



import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

// DTO for GET /dashboard/summary
@Data
@NoArgsConstructor
public class AdminDashboardSummaryDTO {

    private BigDecimal totalCollected;      // SUM of all successful Payments
    private BigDecimal totalOutstanding;    // SUM of (total - paid) for PENDING/OVERDUE Invoices
    private BigDecimal totalOverdue;        // SUM of (total - paid) for OVERDUE Invoices
    private Long pendingInvoicesCount;  // COUNT of Invoices with PENDING status
}