package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.QuestionBank;
import com.project.edusync.em.model.enums.DifficultyLevel;
import com.project.edusync.em.model.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {

    Optional<QuestionBank> findByUuid(UUID uuid);

    @Query("SELECT q FROM QuestionBank q WHERE " +
            "(:subjectUuid IS NULL OR q.subject.uuid = :subjectUuid) AND " +
            "(:classUuid IS NULL OR q.academicClass.uuid = :classUuid) AND " +
            "(CAST(:topic AS string) IS NULL OR LOWER(q.topic) LIKE LOWER(CONCAT('%', CAST(:topic AS string), '%'))) AND " +
            "(:qType IS NULL OR q.questionType = :qType) AND " +
            "(:diffLevel IS NULL OR q.difficultyLevel = :diffLevel)")
    List<QuestionBank> findAllByFilters(
            @Param("subjectUuid") UUID subjectUuid,
            @Param("classUuid") UUID classUuid,
            @Param("topic") String topic,
            @Param("qType") QuestionType qType,
            @Param("diffLevel") DifficultyLevel diffLevel
    );
}