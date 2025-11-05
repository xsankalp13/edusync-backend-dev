package com.project.edusync.em.model.repository;


import com.project.edusync.em.model.entity.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    Optional<QuestionBank> findByUuid(UUID uuid);
}
