package com.project.edusync.adm.repository;

import com.project.edusync.adm.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("SELECT r FROM Room r WHERE r.isActive = true")
    List<Room> findAllActive();

    @Query("SELECT r FROM Room r WHERE r.uuid = :roomId AND r.isActive = true")
    Optional<Room> findActiveById(UUID roomId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r WHERE r.uuid = :roomId AND r.isActive = true")
    boolean existsActiveById(UUID roomId);

    @Transactional
    @Modifying
    @Query("UPDATE Room r SET r.isActive = false WHERE r.uuid = :roomId")
    void softDeleteById(UUID roomId);

    /**
     * Checks if a room exists with the given name, excluding a room with the given UUID.
     * This is crucial for update validation.
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Room r " +
            "WHERE r.name = :name AND r.uuid != :excludeUuid AND r.isActive = true")
    boolean existsByNameAndUuidNot(String name, UUID excludeUuid);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Room r " +
            "WHERE LOWER(r.name) = LOWER(:name) AND r.uuid != :excludeUuid AND r.isActive = true")
    boolean existsByNameIgnoreCaseAndUuidNot(String name, UUID excludeUuid);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Room r " +
            "WHERE r.name = :name AND r.isActive = true")
    boolean existsByName(String name);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Room r " +
            "WHERE LOWER(r.name) = LOWER(:name) AND r.isActive = true")
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Room r " +
            "WHERE r.building.uuid = :buildingId")
    boolean existsByBuildingUuid(UUID buildingId);

    /**
     * Finds available rooms of a specific type for a given timeslot.
     */
    @Query("SELECT r FROM Room r " +
            "WHERE r.isActive = true " +
            "AND r.roomType = :roomType " +
            "AND r.uuid NOT IN (" +
            "  SELECT s.room.uuid FROM Schedule s " +
            "  WHERE s.timeslot.uuid = :timeslotId AND s.isActive = true" +
            ")")
    List<Room> findAvailableRoomsByType(UUID timeslotId, String roomType);

    /**
     * Finds all available rooms for a given timeslot.
     */
    @Query("SELECT r FROM Room r " +
            "WHERE r.isActive = true " +
            "AND r.uuid NOT IN (" +
            "  SELECT s.room.uuid FROM Schedule s " +
            "  WHERE s.timeslot.uuid = :timeslotId AND s.isActive = true" +
            ")")
    List<Room> findAvailableRooms(UUID timeslotId);
}