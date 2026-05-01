package com.project.edusync.finance.repository;

import com.project.edusync.finance.model.entity.ScholarshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ScholarshipTypeRepository extends JpaRepository<ScholarshipType, Long> {
    
    @Modifying
    @Query("UPDATE ScholarshipType st SET st.totalDiscountIssued = COALESCE(st.totalDiscountIssued, 0) + :amount WHERE st.id = :id")
    void incrementTotalDiscountIssued(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
