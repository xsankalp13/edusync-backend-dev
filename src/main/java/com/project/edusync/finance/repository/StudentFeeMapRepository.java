package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.StudentFeeMap;
import com.project.edusync.uis.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link StudentFeeMap} entity.
 */
@Repository
public interface StudentFeeMapRepository extends JpaRepository<StudentFeeMap, Long> {

    /**
     * Finds all fee mappings for a specific student.
     *
     * @param student The Student entity.
     * @return A list of fee mappings for that student.
     */
    List<StudentFeeMap> findByStudent(Student student);

    /**
     * Finds a student's fee mapping by their student ID.
     * This assumes a student is typically mapped to only one active structure.
     *
     * @param id The ID of the student.
     * @return An Optional containing the mapping if found.
     */
    Optional<StudentFeeMap> findByStudent_Id(Long id);
    // Note: If your Student's PK is 'id' (a Long), this should be findByStudent_Id(Long studentId)
    /**
     * Checks if a mapping already exists for a specific student and fee structure.
     *
     * @param studentId   The ID of the student.
     * @param structureId The ID of the fee structure.
     * @return true if a mapping exists, false otherwise.
     */
    boolean existsByStudent_IdAndFeeStructure_Id(Long studentId, Long structureId);
}
