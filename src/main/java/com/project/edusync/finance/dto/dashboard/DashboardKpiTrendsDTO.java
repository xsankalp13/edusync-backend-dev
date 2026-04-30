package com.project.edusync.finance.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for GET /auth/dashboard/kpi-trends.
 *
 * Returns period-comparison deltas so the frontend can show real
 * percentage changes instead of hardcoded values.
 *
 * Periods:
 *   MTD  = current month start → today   vs   prior month same span
 *   YTD  = current year Jan-1 → today    vs   prior year same span
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiTrendsDTO {

    /** Month-to-date collected revenue */
    private BigDecimal revenueMtd;
    /** Same span in the prior month */
    private BigDecimal revenuePriorMtd;
    /** Percentage change (positive = up, negative = down) */
    private double revenueDeltaPct;

    /** Outstanding (expected − collected) for MTD */
    private BigDecimal outstandingMtd;
    /** Outstanding for the prior month MTD */
    private BigDecimal outstandingPriorMtd;
    /** Percentage change */
    private double outstandingDeltaPct;

    /** Number of pending invoices today */
    private long pendingInvoiceCount;

    /** Payroll outflow MTD (PROCESSED | APPROVED | DISBURSED runs) */
    private BigDecimal payrollMtd;
    /** Payroll outflow prior month MTD */
    private BigDecimal payrollPriorMtd;
    /** Percentage change */
    private double payrollDeltaPct;
}
