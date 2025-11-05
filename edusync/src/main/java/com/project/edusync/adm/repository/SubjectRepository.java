package com.project.edusync.adm.repository;

import com.project.edusync.adm.model.entity.Subject;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Query("SELECT s FROM Subject s WHERE s.uuid = :subjectId AND s.isActive = true")
    Optional<Subject> findActiveById(UUID subjectId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subject s WHERE s.uuid = :subjectId AND s.isActive = true")
    boolean existsActiveById(UUID subjectId);

    @Transactional
    @Modifying // <-- This is required for update/delete queries
    @Query("UPDATE Subject s SET s.isActive = false WHERE s.uuid = :subjectId")
    void softDeleteById(UUID subjectId);

    /**
     * Checks if a subject exists with the given code, excluding a subject with the given UUID.
     * This is crucial for update validation.
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Subject s " +
            "WHERE s.subjectCode = :subjectCode AND s.uuid != :excludeUuid AND s.isActive = true")
    boolean existsBySubjectCodeAndUuidNot(String subjectCode, UUID excludeUuid);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Subject s " +
            "WHERE s.subjectCode = :subjectCode AND s.isActive = true")
    boolean existsBySubjectCode(String subjectCode);
}
