package com.project.edusync.adm.repository;

import com.project.edusync.adm.model.entity.Timeslot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimeslotRepository extends JpaRepository<Timeslot, Long> {

    @Query("SELECT t FROM Timeslot t WHERE t.uuid = :timeslotId AND t.isActive = true")
    Optional<Timeslot> findActiveById(UUID timeslotId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Timeslot t WHERE t.uuid = :timeslotId AND t.isActive = true")
    boolean existsActiveById(UUID timeslotId);

    @Transactional
    @Modifying
    @Query("UPDATE Timeslot t SET t.isActive = false WHERE t.uuid = :timeslotId")
    void softDeleteById(UUID timeslotId);

    // --- Filtering ---
    @Query("SELECT t FROM Timeslot t WHERE t.isActive = true")
    List<Timeslot> findAllActive();

    @Query("SELECT t FROM Timeslot t WHERE t.isActive = true AND t.dayOfWeek = :dayOfWeek")
    List<Timeslot> findAllActiveByDayOfWeek(Short dayOfWeek);

    // --- Uniqueness Checks ---
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM Timeslot t " +
            "WHERE t.dayOfWeek = :dayOfWeek AND t.startTime = :startTime AND t.isActive = true")
    boolean existsByDayOfWeekAndStartTime(Short dayOfWeek, LocalTime startTime);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM Timeslot t " +
            "WHERE t.dayOfWeek = :dayOfWeek AND t.startTime = :startTime AND t.uuid != :excludeUuid AND t.isActive = true")
    boolean existsByDayOfWeekAndStartTimeAndUuidNot(Short dayOfWeek, LocalTime startTime, UUID excludeUuid);
}