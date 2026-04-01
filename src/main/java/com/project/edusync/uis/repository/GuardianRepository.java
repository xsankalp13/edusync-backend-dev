package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Optional<Guardian> findByUserProfile(UserProfile userProfile);
    Optional<Guardian> findByPhoneNumber(String phoneNumber);
    Optional<Guardian> findByUserProfile_User_Id(Long userId);
    Optional<Guardian> findByUuid(UUID uuid);

    @Query(value = """
            SELECT g
            FROM Guardian g
            JOIN g.userProfile up
            JOIN up.user u
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(CONCAT(COALESCE(up.firstName, ''), ' ', COALESCE(up.middleName, ''), ' ', COALESCE(up.lastName, ''))) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(g.phoneNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(g)
            FROM Guardian g
            JOIN g.userProfile up
            JOIN up.user u
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(CONCAT(COALESCE(up.firstName, ''), ' ', COALESCE(up.middleName, ''), ' ', COALESCE(up.lastName, ''))) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(g.phoneNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Guardian> findForSuperAdmin(@Param("search") String search, Pageable pageable);
}
