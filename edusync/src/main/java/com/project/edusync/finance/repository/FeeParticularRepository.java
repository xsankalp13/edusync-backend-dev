package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.FeeParticular;
import com.project.edusync.finance.model.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the {@link FeeParticular} entity.
 */
@Repository
public interface FeeParticularRepository extends JpaRepository<FeeParticular, Long> {

    /**
     * Finds all fee particulars associated with a specific fee structure.
     *
     * @param feeStructure The parent FeeStructure entity.
     * @return A list of fee particulars belonging to that structure.
     */
    List<FeeParticular> findByFeeStructure(FeeStructure feeStructure);

    /**
     * Finds all fee particulars for a given structure ID.
     *
     * @param structureId The ID of the parent FeeStructure.
     * @return A list of fee particulars.
     */
    List<FeeParticular> findByFeeStructure_StructureId(Integer structureId);
}