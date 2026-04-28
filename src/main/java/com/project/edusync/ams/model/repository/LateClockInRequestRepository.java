package com.project.edusync.ams.model.repository;

import com.project.edusync.ams.model.entity.LateClockInRequest;
import com.project.edusync.ams.model.enums.LateClockInStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LateClockInRequestRepository extends JpaRepository<LateClockInRequest, Long> {

    Optional<LateClockInRequest> findByUuid(UUID uuid);

    boolean existsByStaffIdAndAttendanceDate(Long staffId, LocalDate attendanceDate);

    @Query("""
            SELECT r FROM LateClockInRequest r
            WHERE (:status IS NULL OR r.status = :status)
            ORDER BY r.attendanceDate DESC, r.createdAt DESC
            """)
    Page<LateClockInRequest> findAllByStatus(
            @Param("status") LateClockInStatus status,
            Pageable pageable);

    long countByStatus(LateClockInStatus status);
}
