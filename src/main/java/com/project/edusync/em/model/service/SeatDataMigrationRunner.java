package com.project.edusync.em.model.service;

import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatDataMigrationRunner implements ApplicationRunner {

    private final RoomRepository roomRepository;
    private final SeatAllocationService seatAllocationService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // ── Phase 1: Legacy room grid migration ─────────────────────
        log.info("Checking for legacy rooms without grid layouts...");
        List<Room> allRooms = roomRepository.findAllActive();
        for (Room room : allRooms) {
            if (room.getRowCount() == null || room.getColumnsPerRow() == null) {
                log.info("Migrating legacy room: {}", room.getName());
                room.setRowCount(1);
                room.setColumnsPerRow(room.getCapacity() != null && room.getCapacity() > 0 ? room.getCapacity() : 50);
                room.setSeatsPerUnit(1);
                roomRepository.save(room);
                
                seatAllocationService.generateSeatsForRoom(room);
                log.info("Successfully generated physical seating grid for room: {}", room.getName());
            }
        }

        // ── Phase 2: Migrate position enum → positionIndex ──────────
        migratePositionToIndex();
    }

    private void migratePositionToIndex() {
        // Check if old 'position' column still exists
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'seat_allocation' AND column_name = 'position'",
                Integer.class);

            if (count != null && count > 0) {
                log.info("Found legacy 'position' column in seat_allocation. Running migration...");

                // Check if position_index already exists
                Integer posIdxExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_name = 'seat_allocation' AND column_name = 'position_index'",
                    Integer.class);

                if (posIdxExists != null && posIdxExists == 0) {
                    // Add the new column
                    jdbcTemplate.execute("ALTER TABLE seat_allocation ADD COLUMN position_index INTEGER");
                    log.info("Added position_index column");
                }

                // Backfill position_index from position
                int updated = jdbcTemplate.update(
                    "UPDATE seat_allocation SET position_index = CASE " +
                    "WHEN position = 'LEFT' THEN 0 " +
                    "WHEN position = 'RIGHT' THEN 1 " +
                    "WHEN position = 'SINGLE' THEN 0 " +
                    "ELSE 0 END " +
                    "WHERE position_index IS NULL");
                log.info("Backfilled {} rows: position → position_index (LEFT=0, RIGHT=1, SINGLE=0)", updated);

                // Set NOT NULL
                jdbcTemplate.execute("ALTER TABLE seat_allocation ALTER COLUMN position_index SET NOT NULL");

                // Drop old unique constraint (if exists)
                try {
                    jdbcTemplate.execute("ALTER TABLE seat_allocation DROP CONSTRAINT IF EXISTS uk_seat_alloc_seat_schedule_position");
                    log.info("Dropped old unique constraint uk_seat_alloc_seat_schedule_position");
                } catch (Exception e) {
                    log.debug("Old constraint already removed or not found: {}", e.getMessage());
                }

                // Drop old index (if exists)
                try {
                    jdbcTemplate.execute("DROP INDEX IF EXISTS idx_seat_alloc_position");
                    log.info("Dropped old index idx_seat_alloc_position");
                } catch (Exception e) {
                    log.debug("Old index already removed or not found: {}", e.getMessage());
                }

                // Drop old position column
                jdbcTemplate.execute("ALTER TABLE seat_allocation DROP COLUMN position");
                log.info("Dropped legacy 'position' column");

                log.info("✅ Position migration complete: position(enum) → position_index(integer)");
            } else {
                log.info("No legacy 'position' column found — migration already complete.");
            }
        } catch (Exception e) {
            log.warn("Position migration check failed (may be first run): {}", e.getMessage());
        }

        // ── Phase 3: Remove seat_side from exam_schedule if exists ──
        try {
            Integer seatSideExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'exam_schedule' AND column_name = 'seat_side'",
                Integer.class);

            if (seatSideExists != null && seatSideExists > 0) {
                jdbcTemplate.execute("ALTER TABLE exam_schedule DROP COLUMN seat_side");
                log.info("Dropped legacy 'seat_side' column from exam_schedule");
            }
        } catch (Exception e) {
            log.debug("seat_side cleanup check failed: {}", e.getMessage());
        }
    }
}
