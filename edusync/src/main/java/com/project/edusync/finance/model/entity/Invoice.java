package com.project.edusync.finance.model.entity;


import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.finance.model.enums.InvoiceStatus;
import com.project.edusync.uis.model.entity.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "invoices")
public class Invoice extends AuditableEntity {



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // ... other fields (invoiceNumber, dates, amounts) are correct ...
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "late_fee_amount", nullable = false, precision = 10, scale = 2)
    @ColumnDefault("0.00")
    private BigDecimal lateFeeAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    @ColumnDefault("0.00")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * --- FIXED ---
     * Removed 'columnDefinition' from the @Column annotation.
     * Note: The @ColumnDefault string must match the Java enum name (e.g., 'PENDING').
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'PENDING'")
    private InvoiceStatus status;

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    public void addLineItem(InvoiceLineItem lineItem) {
        lineItems.add(lineItem);
        lineItem.setInvoice(this);
    }
}
