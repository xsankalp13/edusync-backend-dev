package com.project.edusync.iam.repository;

import com.project.edusync.iam.model.entity.PasswordResetToken;
import com.project.edusync.iam.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.time.Instant;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);

    Optional<PasswordResetToken> findFirstByUserAndExpiryDateAfterOrderByExpiryDateDesc(User user, Instant now);

    long countByUserAndCreatedAtAfter(User user, Instant time);
}