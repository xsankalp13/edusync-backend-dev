package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.dashboard.AdminDashboardSummaryDTO;
import com.project.edusync.finance.dto.dashboard.ParentDashboardSummaryDTO;

/**
 * Service interface for handling dashboard and reporting data.
 */
public interface DashboardService {

    /**
     * Fetches aggregated financial data for the admin dashboard.
     * @return An AdminDashboardSummaryDTO with calculated totals.
     */
    AdminDashboardSummaryDTO getAdminDashboardSummary();

    /**
     * Fetches the financial summary for a single student.
     * TODO: (Temporary method for testing parent logic without auth).
     *
     * @param studentId The ID of the student.
     * @return A ParentDashboardSummaryDTO with calculated totals.
     */
    ParentDashboardSummaryDTO getParentDashboardSummary(Long studentId);
}

