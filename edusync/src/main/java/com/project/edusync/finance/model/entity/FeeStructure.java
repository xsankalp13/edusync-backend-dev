package com.project.edusync.finance.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fee_structures")
@EntityListeners(AuditingEntityListener.class)
public class FeeStructure extends AuditableEntity {


    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "academic_year", nullable = false, length = 10)
    private String academicYear;


    /**
     * @Column(columnDefinition = "TEXT"): Used for database-specific types
     * that don't have a standard JPA equivalent (like 'TEXT' vs 'VARCHAR').
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * @ColumnDefault("false"): This annotation (from Hibernate) sets the
     * 'DEFAULT false' value at the database level.
     */
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("false")
    private boolean isActive;


}
