package com.project.edusync.teacher.repository;

import com.project.edusync.teacher.model.entity.ProxyRequest;
import com.project.edusync.teacher.model.enums.ProxyRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProxyRequestRepository extends JpaRepository<ProxyRequest, Long> {

    /** All requests involving a given user (either sent or received). */
    @Query("""
        SELECT r FROM ProxyRequest r
        WHERE r.requestedBy.id = :userId OR r.requestedTo.id = :userId
        ORDER BY r.createdAt DESC
    """)
    List<ProxyRequest> findAllByUserId(@Param("userId") Long userId);

    /** Requests the user sent (where they are the requester). */
    List<ProxyRequest> findByRequestedByIdOrderByCreatedAtDesc(Long userId);

    /** Requests addressed to the user  (incoming proxy asks). */
    List<ProxyRequest> findByRequestedToIdOrderByCreatedAtDesc(Long userId);

    /** Today's confirmed proxy classes for a user (as the proxy teacher). */
    List<ProxyRequest> findByRequestedToIdAndPeriodDateAndStatus(
            Long userId, LocalDate periodDate, ProxyRequestStatus status);

    /** Count proxy assignments accepted by a teacher between two dates (for load stats). */
    @Query("""
        SELECT COUNT(r) FROM ProxyRequest r
        WHERE r.requestedTo.id = :userId
          AND r.status = 'ACCEPTED'
          AND r.periodDate BETWEEN :from AND :to
    """)
    long countAcceptedProxiesBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** All pending or accepted requests on a given date (for admin dashboard). */
    @Query("""
        SELECT r FROM ProxyRequest r
        WHERE r.periodDate = :date
          AND r.status IN ('PENDING', 'ACCEPTED')
        ORDER BY r.createdAt DESC
    """)
    List<ProxyRequest> findActiveRequestsOnDate(@Param("date") LocalDate date);

    /** Check whether a proxy is already assigned for an absent teacher on a date. */
    boolean existsByRequestedByIdAndPeriodDateAndStatusNot(
            Long requestedById, LocalDate periodDate, ProxyRequestStatus excludedStatus);

    /** Find by uuid. */
    java.util.Optional<ProxyRequest> findByUuid(UUID uuid);

    /**
     * Counts pending proxy requests for a given date.
     * Replaces findActiveRequestsOnDate(...).stream().filter(PENDING).count() pattern
     * which loads all entities into memory.
     */
    @Query("""
        SELECT COUNT(r) FROM ProxyRequest r
        WHERE r.periodDate = :date
          AND r.status = com.project.edusync.teacher.model.enums.ProxyRequestStatus.PENDING
    """)
    long countPendingByDate(@Param("date") LocalDate date);
}
