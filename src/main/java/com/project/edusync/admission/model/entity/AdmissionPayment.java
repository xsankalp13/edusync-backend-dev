package com.project.edusync.admission.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.finance.model.enums.PaymentMethod;
import com.project.edusync.finance.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "admission_payments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdmissionPayment extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    private LocalDateTime paidAt;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;
}
