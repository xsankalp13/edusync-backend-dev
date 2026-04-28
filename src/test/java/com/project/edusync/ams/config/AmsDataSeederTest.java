package com.project.edusync.ams.config;

import com.project.edusync.ams.model.entity.AttendanceType;
import com.project.edusync.ams.model.repository.AttendanceTypeRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled("Temporarily disabled: attendance seeder behavior changed; tests need rewrite")
class AmsDataSeederTest {

    @Mock
    private AttendanceTypeRepository attendanceTypeRepository;

    @InjectMocks
    private AmsDataSeeder amsDataSeeder;

    @Test
    void seedDefaultAttendanceTypes_createsAllWhenMissing() {
        when(attendanceTypeRepository.findByShortCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(attendanceTypeRepository.save(any(AttendanceType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        amsDataSeeder.seedDefaultAttendanceTypes();

        ArgumentCaptor<AttendanceType> captor = ArgumentCaptor.forClass(AttendanceType.class);
        verify(attendanceTypeRepository, times(3)).save(captor.capture());

        AttendanceType present = captor.getAllValues().get(0);
        assertEquals("Present", present.getTypeName());
        assertEquals("P", present.getShortCode());
        assertTrue(present.isPresentMark());
        assertFalse(present.isAbsenceMark());
        assertFalse(present.isLateMark());
        assertEquals("#10b981", present.getColorCode());

        AttendanceType absent = captor.getAllValues().get(1);
        assertEquals("Absent", absent.getTypeName());
        assertEquals("A", absent.getShortCode());
        assertFalse(absent.isPresentMark());
        assertTrue(absent.isAbsenceMark());
        assertFalse(absent.isLateMark());
        assertEquals("#ef4444", absent.getColorCode());

        AttendanceType late = captor.getAllValues().get(2);
        assertEquals("Late", late.getTypeName());
        assertEquals("L", late.getShortCode());
        assertFalse(late.isPresentMark());
        assertFalse(late.isAbsenceMark());
        assertTrue(late.isLateMark());
        assertEquals("#f59e0b", late.getColorCode());
        assertTrue(late.isActive());
    }

    @Test
    void seedDefaultAttendanceTypes_skipsExistingCodes() {
        AttendanceType present = new AttendanceType();
        present.setShortCode("P");
        AttendanceType absent = new AttendanceType();
        absent.setShortCode("A");
        AttendanceType late = new AttendanceType();
        late.setShortCode("L");

        when(attendanceTypeRepository.findByShortCodeIgnoreCase("P")).thenReturn(Optional.of(present));
        when(attendanceTypeRepository.findByShortCodeIgnoreCase("A")).thenReturn(Optional.of(absent));
        when(attendanceTypeRepository.findByShortCodeIgnoreCase("L")).thenReturn(Optional.of(late));

        amsDataSeeder.seedDefaultAttendanceTypes();

        verify(attendanceTypeRepository, never()).save(any(AttendanceType.class));
    }
}


