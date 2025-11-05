package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.model.dto.request.AcademicClassRequestDto;
import com.project.edusync.adm.model.dto.response.AcademicClassResponseDto;
import com.project.edusync.adm.model.dto.response.SectionResponseDto;
import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.AcademicClassRepository;
import com.project.edusync.adm.service.AcademicClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AcademicClassServiceImpl implements AcademicClassService {

    private final AcademicClassRepository academicClassRepository;


    @Override
    public AcademicClassResponseDto addClass(AcademicClassRequestDto academicClassRequestDto){
        log.info("Attempting to create a new class with name: {}", academicClassRequestDto.getName());
        AcademicClass newClass = new AcademicClass();
        newClass.setName(academicClassRequestDto.getName());
        AcademicClass savedClass = academicClassRepository.save(newClass);
        log.info("Class {} created successfully",savedClass.getName());

        return AcademicClassResponseDto.builder()
                .name(savedClass.getName())
                .build();
    }

    @Override
    public List<AcademicClassResponseDto> getAllClasses() {
        log.info("Fetching all academic classes");
        return academicClassRepository.findAll().stream()
                .map(this::toClassResponseDto) // Use private helper for conversion
                .collect(Collectors.toList());
    }

    private AcademicClassResponseDto toClassResponseDto(AcademicClass entity) {
        if (entity == null) return null;
        return AcademicClassResponseDto.builder()
                .classId(entity.getUuid())
                .name(entity.getName())
                .sections(
                        (entity.getSections() != null) ?
                                entity.getSections().stream()
                                        .map(this::toSectionResponseDto)
                                        .collect(Collectors.toSet()) : Set.of()
                )
                .build();
    }

    private SectionResponseDto toSectionResponseDto(Section entity) {
        if (entity == null) return null;
        return SectionResponseDto.builder()
                .uuid(entity.getUuid())
                .sectionName(entity.getSectionName())
                .build();
    }
}
