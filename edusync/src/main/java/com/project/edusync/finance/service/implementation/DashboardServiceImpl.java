package com.project.edusync.finance.service.implementation;

import com.project.edusync.common.exception.finance.StudentNotFoundException;
import com.project.edusync.finance.dto.dashboard.AdminDashboardSummaryDTO;
import com.project.edusync.finance.dto.dashboard.ParentDashboardSummaryDTO;
import com.project.edusync.finance.repository.InvoiceRepository;
import com.project.edusync.finance.repository.PaymentRepository;
import com.project.edusync.finance.service.DashboardService;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummaryDTO getAdminDashboardSummary() {

        // 1. Call all the custom aggregate queries
        BigDecimal totalCollected = paymentRepository.findTotalCollected();
        BigDecimal totalOutstanding = invoiceRepository.findTotalOutstanding();
        BigDecimal totalOverdue = invoiceRepository.findTotalOverdue();
        Long pendingInvoicesCount = invoiceRepository.countPendingInvoices();

        // 2. Assemble the DTO
        AdminDashboardSummaryDTO summaryDTO = new AdminDashboardSummaryDTO();
        summaryDTO.setTotalCollected(totalCollected);
        summaryDTO.setTotalOutstanding(totalOutstanding);
        summaryDTO.setTotalOverdue(totalOverdue);
        summaryDTO.setPendingInvoicesCount(pendingInvoicesCount);

        // 3. Return the DTO
        return summaryDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public ParentDashboardSummaryDTO getParentDashboardSummary(Long studentId) {

        // 1. Verify student exists
        if (!studentRepository.existsById(studentId)) {
            throw new StudentNotFoundException("Student not found with ID: " + studentId);
        }

        // 2. Call the new aggregate queries
        BigDecimal totalDue = invoiceRepository.findTotalDueForStudent(studentId);
        LocalDate nextDueDate = invoiceRepository.findNextDueDateForStudent(studentId)
                .orElse(null); // Return null if no upcoming due dates

        // 3. Assemble the DTO
        ParentDashboardSummaryDTO summaryDTO = new ParentDashboardSummaryDTO();
        summaryDTO.setTotalDue(totalDue);
        summaryDTO.setNextDueDate(nextDueDate);

        return summaryDTO;
    }
}