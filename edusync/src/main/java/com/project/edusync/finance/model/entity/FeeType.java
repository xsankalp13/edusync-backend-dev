package com.project.edusync.finance.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fee_types")
public class FeeType {


    /**
     * @Id: Marks this field as the primary key.
     * @GeneratedValue(strategy = GenerationType. IDENTITY): Configures the way the
     * ID is generated. 'IDENTITY' delegates the responsibility of
     * auto-incrementing the value to the database (matches `AUTO_INCREMENT`).
     * @Column(name = "fee_type_id"): Maps this field to the 'fee_type_id' column.
     * This is technically optional if the field and column names match,
     * but it's good practice to be explicit.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_type_id")
    private Long feeTypeId;

    /**
     * @Column(...): Maps this field to the 'type_name' column.
     * - nullable = false: Corresponds to the 'NOT NULL' constraint.
     * - unique = true: Corresponds to the 'UNIQUE' constraint.
     * - length = 50: Corresponds to 'VARCHAR(50)'.
     */
    @Column(name = "type_name", nullable = false, unique = true, length = 50)
    private String typeName;

    /**
     * @Column(...): Maps this field to the 'description' column.
     * - length = 255: Corresponds to 'VARCHAR(255)'.
     * - 'nullable = true' is the default, so we don't need to specify it
     * for this 'NULL' column.
     */
    @Column(name = "description", length = 255)
    private String description;

    @Column(name="is_active)", nullable = false)
    @ColumnDefault("true")
    private Boolean isActive;
}
