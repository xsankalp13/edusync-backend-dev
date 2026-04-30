package com.project.edusync.finance.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Predictive intelligence payload for the admin dashboard.
 * Provides three forecast signals:
 *   1. Revenue EOM projection  — will we hit this month's fee target?
 *   2. Staff attendance trend  — is presence improving, stable, or declining?
 *   3. Outstanding risk level  — is unpaid balance growing month-over-month?
 */
@Getter
@Builder
public class DashboardForecastDTO {

    // ── Revenue Forecast ─────────────────────────────────────────────────
    /** Extrapolated end-of-month revenue based on current daily collection rate. */
    private BigDecimal revenueEomForecast;

    /** Total invoiced amount expected for this full calendar month. */
    private BigDecimal revenueMonthTarget;

    /** Projected collection as a percentage of the month target. */
    private double revenueTrajectoryPct;

    /** ON_TRACK (≥90%), AT_RISK (70–89%), CRITICAL (<70%). */
    private String revenueTrajectory;

    // ── Attendance Trend ─────────────────────────────────────────────────
    /** IMPROVING, STABLE, or DECLINING based on 7-day rolling window. */
    private String attendanceTrend;

    /** Average daily change in staff attendance % (positive = improving). */
    private double attendanceTrendSlope;

    /** Most recent day's staff attendance percentage. */
    private double currentStaffAttendancePct;

    // ── Outstanding Risk ─────────────────────────────────────────────────
    /** LOW (<5% MoM growth), MEDIUM (5–20%), HIGH (>20%). */
    private String outstandingRisk;

    /** Month-over-month percentage change in net outstanding balance. */
    private double outstandingGrowthRate;
}
