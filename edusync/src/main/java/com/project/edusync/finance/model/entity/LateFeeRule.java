package com.project.edusync.finance.model.entity;


import com.project.edusync.finance.model.enums.FineType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "late_fee_rules")
public class LateFeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "days_after_due", nullable = false)
    private Integer daysAfterDue;

    /**
     * --- FIXED ---
     * Removed 'columnDefinition' from the @Column annotation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fine_type", nullable = false)
    @ColumnDefault("'FIXED'")
    private FineType fineType;

    @Column(name = "fine_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineValue;

    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id", nullable = true)
    private FeeStructure feeStructure;
}
