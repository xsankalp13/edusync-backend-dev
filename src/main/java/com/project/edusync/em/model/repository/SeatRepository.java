package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * All seats for a room, ordered row-wise then column-wise.
     */
    List<Seat> findByRoomIdOrderByRowNumberAscColumnNumberAsc(Long roomId);

    /**
     * Total physical seats in a room.
     */
    long countByRoomId(Long roomId);

    /**
     * Batch count of seats per room (avoids N+1 when listing all rooms).
     */
    @Query("""
        SELECT s.room.id, COUNT(s)
        FROM Seat s
        GROUP BY s.room.id
        """)
    List<Object[]> countSeatsPerRoom();

    /**
     * Safety check: are there any SeatAllocations referencing seats in this room?
     * Must be checked before regenerating or deleting seats.
     */
    @Query("""
        SELECT CASE WHEN COUNT(sa) > 0 THEN true ELSE false END
        FROM SeatAllocation sa
        WHERE sa.seat.room.id = :roomId
        """)
    boolean existsAllocationsByRoomId(@Param("roomId") Long roomId);

    /**
     * Delete all seats for a room (used during seat regeneration).
     */
    @Modifying
    void deleteAllByRoomId(Long roomId);
}
