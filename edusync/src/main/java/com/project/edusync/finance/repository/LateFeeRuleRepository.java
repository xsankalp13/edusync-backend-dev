package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.LateFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the {@link LateFeeRule} entity.
 */
@Repository
public interface LateFeeRuleRepository extends JpaRepository<LateFeeRule, Long> {

    /**
     * Finds all active (or inactive) late fee rules.
     *
     * @param isActive The active status.
     * @return A list of matching rules.
     */
    List<LateFeeRule> findByIsActive(boolean isActive);
}