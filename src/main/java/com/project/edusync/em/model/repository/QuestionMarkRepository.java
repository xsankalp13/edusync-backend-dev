package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.QuestionMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionMarkRepository extends JpaRepository<QuestionMark, Long> {
    List<QuestionMark> findByEvaluationResultIdOrderBySectionNameAscQuestionNumberAscOptionLabelAsc(Long evaluationResultId);

    Optional<QuestionMark> findByEvaluationResultIdAndSectionNameAndQuestionNumberAndOptionLabel(Long evaluationResultId,
                                                                                                 String sectionName,
                                                                                                 Integer questionNumber,
                                                                                                 String optionLabel);

    @Modifying
    @Query("DELETE FROM QuestionMark qm WHERE qm.evaluationResult.id = :resultId")
    void deleteByEvaluationResultId(@Param("resultId") Long resultId);
}

