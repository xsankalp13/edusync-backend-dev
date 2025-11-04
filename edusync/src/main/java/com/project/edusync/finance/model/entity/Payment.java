package com.project.edusync.finance.model.entity;




import com.project.edusync.finance.model.enums.PaymentMethod;
import com.project.edusync.finance.model.enums.PaymentStatus;
import com.project.edusync.uis.model.entity.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // ... other fields (transactionId, paymentDate, amountPaid) are correct ...
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "payment_date", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime paymentDate;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    /**
     * --- FIXED ---
     * Removed 'columnDefinition' from the @Column annotation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    @ColumnDefault("'ONLINE'")
    private PaymentMethod paymentMethod;

    /**
     * --- FIXED ---
     * Removed 'columnDefinition' from the @Column annotation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'SUCCESS'")
    private PaymentStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
