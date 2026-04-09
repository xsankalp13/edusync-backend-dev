package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.PastPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PastPaperRepository extends JpaRepository<PastPaper, Long> {

    Optional<PastPaper> findByUuid(UUID uuid);

    Optional<PastPaper> findByUuidAndAcademicClass_Uuid(UUID uuid, UUID classUuid);

    @Query("SELECT p FROM PastPaper p WHERE " +
            "(:classUuid IS NULL OR p.academicClass.uuid = :classUuid) AND " +
            "(:subjectUuid IS NULL OR p.subject.uuid = :subjectUuid) AND " +
            "(:year IS NULL OR p.examYear = :year)")
    List<PastPaper> findAllByFilters(
            @Param("classUuid") UUID classUuid,
            @Param("subjectUuid") UUID subjectUuid,
            @Param("year") Integer year
    );
}