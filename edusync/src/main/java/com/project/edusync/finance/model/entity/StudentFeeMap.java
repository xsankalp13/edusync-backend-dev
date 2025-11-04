package com.project.edusync.finance.model.entity;

import com.project.edusync.uis.model.entity.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_fee_maps")
public class StudentFeeMap {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_id")
    private Long mapId;

    /**
     * It is now a direct, many-to-one relationship.
     * JPA will map this to the 'student_id' column by default.
     * We use @JoinColumn to be explicit.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * This is a "real" foreign key within the same 'finance' schema.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id", nullable = false)
    private FeeStructure feeStructure;

    /**
     * Maps to the DATE column. java.time.LocalDate is the modern
     * Java class for dates without time.
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
