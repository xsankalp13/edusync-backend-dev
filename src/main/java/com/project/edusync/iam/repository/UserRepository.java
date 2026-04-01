package com.project.edusync.iam.repository;

import com.project.edusync.iam.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(UUID uuid);

    /**
     * Finds a user by their username and eagerly fetches their roles
     * and the permissions associated with those roles in a single query.
     * This is the most efficient way to load the full security context.
     *
     * @param username The username to search for.
     * @return An Optional containing the User if found.
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions p " +
            "WHERE u.username = :username")
    Optional<User> findByUsernameWithAuthorities(@Param("username") String username);

    boolean existsByEmail(String email);

    boolean existsByRoles_Name(String roleName);

    @Query("SELECT u.email FROM User u WHERE u.email IN :emails")
    Set<String> findEmailsThatExist(@Param("emails") Set<String> emails);

    boolean existsByUsername(String enrollmentNumber);

    // Custom Query to efficiently fetch all role
    //
    // names for a user
    @Query("SELECT r.name FROM User u JOIN u.roles r WHERE u.id = :userId")
    Set<String> findRoleNamesByUserId(@Param("userId") Long userId);
}
