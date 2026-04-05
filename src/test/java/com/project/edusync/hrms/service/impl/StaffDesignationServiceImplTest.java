package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.dto.designation.StaffDesignationCreateUpdateDTO;
import com.project.edusync.hrms.dto.designation.StaffDesignationResponseDTO;
import com.project.edusync.hrms.model.entity.StaffDesignation;
import com.project.edusync.hrms.repository.StaffDesignationRepository;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffDesignationServiceImplTest {

    @Mock
    private StaffDesignationRepository staffDesignationRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private StaffDesignationServiceImpl service;

    @Test
    void createRejectsDuplicateCode() {
        StaffDesignationCreateUpdateDTO dto = new StaffDesignationCreateUpdateDTO(
                "PRT",
                "Primary Teacher",
                StaffCategory.TEACHING,
                null,
                10
        );

        when(staffDesignationRepository.existsByDesignationCodeIgnoreCaseAndActiveTrue("PRT")).thenReturn(true);

        assertThrows(EdusyncException.class, () -> service.create(dto));
    }

    @Test
    void createNormalizesCode() {
        StaffDesignationCreateUpdateDTO dto = new StaffDesignationCreateUpdateDTO(
                " prt ",
                "Primary Teacher",
                StaffCategory.TEACHING,
                null,
                10
        );

        when(staffDesignationRepository.existsByDesignationCodeIgnoreCaseAndActiveTrue("PRT")).thenReturn(false);
        when(staffDesignationRepository.save(any(StaffDesignation.class))).thenAnswer(invocation -> {
            StaffDesignation designation = invocation.getArgument(0);
            designation.setId(1L);
            return designation;
        });

        StaffDesignationResponseDTO response = service.create(dto);
        assertEquals("PRT", response.designationCode());
    }
}

