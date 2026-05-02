package com.project.edusync.finance.dto.invoice;

import com.project.edusync.finance.model.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Used for GET /invoices, GET /invoices/{invoiceId} (Admin)
// and GET /invoices, GET /invoices/{invoiceId} (Parent) [cite: 21]
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDTO {
    private Long invoiceId;
    private Long studentId;
    private String studentName;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal lateFeeAmount;
    private BigDecimal paidAmount;
    private InvoiceStatus status; // 'DRAFT', 'PENDING', 'PAID', 'OVERDUE'
    private List<InvoiceLineItemResponseDTO> lineItems;
    private LocalDateTime createdAt;
    // Getters and Setters
}