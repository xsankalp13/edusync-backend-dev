package com.project.edusync.em.model.service;

import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatDataMigrationRunner implements ApplicationRunner {

    private final RoomRepository roomRepository;
    private final SeatAllocationService seatAllocationService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking for legacy rooms without grid layouts...");
        List<Room> allRooms = roomRepository.findAllActive();
        for (Room room : allRooms) {
            if (room.getRowCount() == null || room.getColumnsPerRow() == null) {
                log.info("Migrating legacy room: {}", room.getName());
                // Fallback to 1 row x capacity columns to satisfy the grid requirements
                room.setRowCount(1);
                room.setColumnsPerRow(room.getCapacity() != null && room.getCapacity() > 0 ? room.getCapacity() : 50);
                room.setSeatsPerUnit(1);
                roomRepository.save(room);
                
                seatAllocationService.generateSeatsForRoom(room);
                log.info("Successfully generated physical seating grid for room: {}", room.getName());
            }
        }
    }
}
