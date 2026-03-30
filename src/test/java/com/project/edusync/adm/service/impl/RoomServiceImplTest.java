package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.exception.AlreadyBookedException;
import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.repository.BuildingRepository;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.adm.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    @Test
    void deleteRoom_throwsNotFound_whenRoomDoesNotExist() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.existsActiveById(roomId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> roomService.deleteRoom(roomId));
        verify(roomRepository, never()).softDeleteById(roomId);
    }

    @Test
    void deleteRoom_throwsConflict_whenRoomMappedInActiveTimetable() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.existsActiveById(roomId)).thenReturn(true);
        when(scheduleRepository.existsActiveByRoomUuid(roomId)).thenReturn(true);

        assertThrows(AlreadyBookedException.class, () -> roomService.deleteRoom(roomId));
        verify(roomRepository, never()).softDeleteById(roomId);
    }

    @Test
    void deleteRoom_softDeletes_whenNoActiveTimetableMapping() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.existsActiveById(roomId)).thenReturn(true);
        when(scheduleRepository.existsActiveByRoomUuid(roomId)).thenReturn(false);

        roomService.deleteRoom(roomId);

        verify(roomRepository).softDeleteById(roomId);
    }
}

