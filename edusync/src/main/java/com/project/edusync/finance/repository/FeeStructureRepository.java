package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link FeeStructure} entity.
 */
@Repository
public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    /**
     * Finds a fee structure by its unique name.
     *
     * @param name The name of the structure (e.g., "Grade 10 - 2025").
     * @return An Optional containing the found FeeStructure, or empty if not found.
     */
    Optional<FeeStructure> findByName(String name);

    /**
     * Finds all fee structures for a specific academic year.
     *
     * @param academicYear The academic year (e.g., "2025-26").
     * @return A list of matching fee structures.
     */
    List<FeeStructure> findByAcademicYear(String academicYear);
}
