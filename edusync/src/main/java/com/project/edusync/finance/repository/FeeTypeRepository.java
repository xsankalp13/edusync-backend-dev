package com.project.edusync.finance.repository;



import com.project.edusync.finance.model.entity.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link FeeType} entity.
 */
@Repository
public interface FeeTypeRepository extends JpaRepository<FeeType, Long> {

    /**
     * Finds a fee type by its unique type name.
     *
     * @param typeName The name of the fee type (e.g., "TUITION").
     * @return An Optional containing the found FeeType, or empty if not found.
     */
    Optional<FeeType> findByTypeName(String typeName);

    /**
     * This is essential for soft-delete.
     */
    List<FeeType> findByIsActive(boolean isActive);
}
