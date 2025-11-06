package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.dashboard.AdminDashboardSummaryDTO;
import com.project.edusync.finance.dto.dashboard.ParentDashboardSummaryDTO;
import com.project.edusync.finance.dto.invoice.InvoiceResponseDTO;
import com.project.edusync.finance.service.DashboardService;
import com.project.edusync.finance.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/finance/dashboard") // Base path: /api/v1/finance/dashboard
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final InvoiceService invoiceService;

    /**
     * GET /api/v1/finance/dashboard/summary
     * Fetches aggregated financial data for the admin dashboard.
     */
    @GetMapping("/summary")
    public ResponseEntity<AdminDashboardSummaryDTO> getAdminDashboardSummary() {
        AdminDashboardSummaryDTO summary = dashboardService.getAdminDashboardSummary();
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }

    /**
     * NEW: GET /api/v1/finance/parent/dashboard/summary/for-student/{studentId}
     * (TEMPORARY ENDPOINT) Fetches the financial summary for a specific student.
     * This will be replaced by a secure GET /dashboard/summary endpoint.
     */
    @GetMapping("/dashboard/summary/for-student/{studentId}")
    public ResponseEntity<ParentDashboardSummaryDTO> getParentDashboardSummary(
            @PathVariable Long studentId) {

        ParentDashboardSummaryDTO summary = dashboardService.getParentDashboardSummary(studentId);
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }


    /**
     * GET /api/v1/finance/parent/invoices/for-student/{studentId}
     * (TEMPORARY ENDPOINT) Retrieves all invoices for a specific student.
     */
    @GetMapping("/invoices/for-student/{studentId}")
    public ResponseEntity<List<InvoiceResponseDTO>> getInvoicesForStudent(
            @PathVariable Long studentId) {

        List<InvoiceResponseDTO> responseList = invoiceService.getInvoicesForStudent(studentId);
        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }

    /**
     * GET /api/v1/finance/parent/invoices/{invoiceId}
     * Gets the detailed line-item breakdown of a specific invoice.
     * (WARNING: Insecure, will be updated later)
     */
    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<InvoiceResponseDTO> getInvoiceByIdForParent(
            @PathVariable Long invoiceId) {

        InvoiceResponseDTO response = invoiceService.getInvoiceByIdForParent(invoiceId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}