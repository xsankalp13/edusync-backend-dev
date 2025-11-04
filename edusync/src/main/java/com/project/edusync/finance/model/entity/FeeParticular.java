package com.project.edusync.finance.model.entity;


import com.project.edusync.finance.model.enums.Frequency;
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
@Table(name = "fee_particulars")
public class FeeParticular {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "particular_id")
    private Long particularId;

    /**
     * @ManyToOne: Defines a many-to-one relationship. Many 'particulars'
     * can belong to one 'structure'.
     * @JoinColumn(name = "structure_id"): Specifies that the 'structure_id'
     * column in *this* table (fee_particulars) is the foreign key.
     * - fetch = FetchType.LAZY: A performance optimization. It tells JPA
     * to only load the associated FeeStructure object from the database
     * when the getFeeStructure() method is explicitly called.
     * The default is EAGER, which can be bad for performance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id", nullable = false)
    private FeeStructure feeStructure;

    /**
     * @ManyToOne: Defines the relationship to FeeType.
     * @JoinColumn: Specifies the 'fee_type_id' column as the foreign key.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_type_id", nullable = false)
    private FeeType feeType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * @Column(precision = 10, scale = 2): Used for DECIMAL types.
     * - precision: The total number of digits (e.g., 10 for 12345678.90).
     * - scale: The number of digits after the decimal point (e.g., 2).
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Removed 'columnDefinition' from the @Column annotation.
     * Hibernate will now default to VARCHAR(255).
     * Note: The @ColumnDefault string must match the Java enum name (e.g., 'ONE_TIME').
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    @ColumnDefault("'ONE_TIME'")
    private Frequency frequency;
}