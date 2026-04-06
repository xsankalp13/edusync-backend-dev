package com.project.edusync.adm.repository;

import com.project.edusync.adm.model.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, Long> {
    @Query("SELECT s FROM Section s JOIN s.academicClass ac WHERE ac.name = :className AND s.sectionName = :sectionName")
    Optional<Section> findByAcademicClass_NameAndSectionName(String className, String sectionName);

    @Query("SELECT s FROM Section s JOIN FETCH s.academicClass")
    List<Section> findAllWithClass();

    @Query("""
            SELECT s FROM Section s
            JOIN FETCH s.academicClass ac
            LEFT JOIN FETCH s.defaultRoom dr
            LEFT JOIN FETCH s.classTeacher ct
            LEFT JOIN FETCH ct.userProfile up
            WHERE s.classTeacher.id = :staffId
              AND s.isActive = true
            """)
    List<Section> findAllActiveByClassTeacherId(@Param("staffId") Long staffId);

    @Query("""
            SELECT s FROM Section s
            JOIN FETCH s.academicClass ac
            LEFT JOIN FETCH s.defaultRoom dr
            LEFT JOIN FETCH s.classTeacher ct
            LEFT JOIN FETCH ct.userProfile up
            WHERE s.classTeacher.id = :staffId
              AND s.isActive = true
            """)
    Optional<Section> findActiveHomeroomByClassTeacherId(Long staffId);

    @Query("SELECT s FROM Section s where s.uuid = :sectionId")
    Optional<Section> findById(UUID sectionId);

    boolean existsByUuid(UUID sectionId);

    @Transactional
    @Modifying
    @Query("UPDATE Section s SET s.isActive = false WHERE s.uuid = :sectionId")
    void softDeleteById(UUID sectionId);

    Optional<Section> findByUuid(UUID uuid);
}
