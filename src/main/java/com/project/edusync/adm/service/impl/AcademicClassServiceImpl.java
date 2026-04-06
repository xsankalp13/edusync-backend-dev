package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.model.dto.request.AcademicClassRequestDto;
import com.project.edusync.adm.model.dto.request.SectionRequestDto;
import com.project.edusync.adm.model.dto.response.AcademicClassResponseDto;
import com.project.edusync.adm.model.dto.response.RoomBasicResponseDto;
import com.project.edusync.adm.model.dto.response.SectionResponseDto;
import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.AcademicClassRepository;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.adm.service.AcademicClassService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AcademicClassServiceImpl implements AcademicClassService {

    private final AcademicClassRepository academicClassRepository;
    private final SectionRepository sectionRepository;
    private final RoomRepository roomRepository;
    private final StaffRepository staffRepository;


    @Override
    public AcademicClassResponseDto addClass(AcademicClassRequestDto academicClassRequestDto) {
        log.info("Attempting to create a new class with name: {}", academicClassRequestDto.getName());
        AcademicClass newClass = new AcademicClass();
        newClass.setName(academicClassRequestDto.getName());
        AcademicClass savedClass = academicClassRepository.save(newClass);
        log.info("Class {} created successfully", savedClass.getName());

        return toClassResponseDto(savedClass);
    }

    @Override
    public List<AcademicClassResponseDto> getAllClasses() {
        log.info("Fetching all academic classes");
        return academicClassRepository.findAll().stream()
                .map(this::toClassResponseDto) // Use private helper for conversion
                .collect(Collectors.toList());
    }

    @Override
    public AcademicClassResponseDto getClassById(UUID classId) {
        log.info("Fetching academic class with id: {}", classId);
        AcademicClass academicClass = academicClassRepository.findById(classId)
                .orElseThrow(() -> {
                    log.warn("No class with id {} found", classId);
                    return new ResourceNotFoundException("No class found with id: " + classId);
                });
        return toClassResponseDto(academicClass); // Use private helper
    }

    @Override
    public AcademicClassResponseDto updateClass(UUID classId, AcademicClassRequestDto academicClassRequestDto) {
        log.info("Attempting to update class with id: {}", classId);
        AcademicClass existingClass = academicClassRepository.findById(classId)
                .orElseThrow(() -> {
                    log.warn("No class with id {} to update", classId);
                    return new ResourceNotFoundException("No resource found to update");
                });
        existingClass.setName(academicClassRequestDto.getName());

        AcademicClass updatedClass = academicClassRepository.save(existingClass);
        log.info("Class with id {} updated successfully", updatedClass.getId());

        return toClassResponseDto(updatedClass); // Use private helper
    }

    @Override
    public void deleteClass(UUID classId) {
        log.info("Attempting to delete class with id: {}", classId);
        if (!academicClassRepository.existsById(classId)) {
            log.warn("Failed to delete. Class not found with id: {}", classId);
            throw new ResourceNotFoundException("AcademicClass id: " + classId + " not found");
        }
        // Add logic here to check for child sections if cascade delete is not on
        academicClassRepository.softDeleteById(classId);
        log.info("Class with id {} deleted successfully", classId);
    }

    @Override
    public SectionResponseDto addSectionToClass(UUID classId, SectionRequestDto sectionRequestDto) {
        log.info("Attempting to add section '{}' to class with id: {}", sectionRequestDto.getSectionName(), classId);
        AcademicClass parentClass = academicClassRepository.findById(classId)
                .orElseThrow(() -> {
                    log.warn("class with id {} not found",classId);
                    return new ResourceNotFoundException("Cannot add section as class with id " + classId + " not found");
                });

        Section newSection = new Section();
        newSection.setSectionName(sectionRequestDto.getSectionName());
        newSection.setAcademicClass(parentClass); // Link to parent
        newSection.setDefaultRoom(resolveDefaultRoom(sectionRequestDto.getDefaultRoomId()));
        newSection.setClassTeacher(resolveClassTeacher(sectionRequestDto.getClassTeacherUuid()));

        Section savedSection = sectionRepository.save(newSection);
        log.info("Section '{}' created successfully with id {} for class id {}", savedSection.getSectionName(), savedSection.getId(), classId);

        // Building response DTO for the new section
        return toSectionResponseDto(savedSection);
    }

    @Override
    public Set<SectionResponseDto> getAllSectionsForClass(UUID classId) {
        log.info("Fetching all sections for class id: {}", classId);
        AcademicClass parentClass = academicClassRepository.findById(classId)
                .orElseThrow(() -> {
                    log.warn("No class with id {} found ", classId);
                    return new ResourceNotFoundException("No class found with id: " + classId);
                });
        return parentClass.getSections().stream()
                .map(this::toSectionResponseDto) // Use private helper
                .collect(Collectors.toSet());
    }

    @Override
    public SectionResponseDto getSectionById(UUID sectionId) {
        log.info("Fetching section with id: {}", sectionId);
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> {
                    log.warn("Section with id {} not found ",sectionId);
                    return new ResourceNotFoundException("no such section found");
                });
        return toSectionResponseDto(section); // Use private helper
    }

    @Override
    public SectionResponseDto updateSection(UUID sectionId, SectionRequestDto sectionRequestDto) {
        log.info("Attempting to update section with id: {}", sectionId);
        Section existingSection = sectionRepository.findById(sectionId)
                .orElseThrow(() -> {
                    log.warn("Section with id {} not found",sectionId);
                    return new ResourceNotFoundException("No section with id: " + sectionId);
                });
        existingSection.setSectionName(sectionRequestDto.getSectionName());
        existingSection.setDefaultRoom(resolveDefaultRoom(sectionRequestDto.getDefaultRoomId()));
        existingSection.setClassTeacher(resolveClassTeacher(sectionRequestDto.getClassTeacherUuid()));

        Section updatedSection = sectionRepository.save(existingSection);
        log.info("Section with id {} updated successfully", updatedSection.getId());

        return toSectionResponseDto(updatedSection);
    }

    @Override
    public void deleteSection(UUID sectionId) {
        log.info("Attempting to delete section with id: {}", sectionId);
        if (!sectionRepository.existsByUuid(sectionId)) {
            log.warn("Failed to delete. Section not found with id: {}", sectionId);
            throw new ResourceNotFoundException("cannot delete as section with id: " + sectionId + " not found");
        }
        sectionRepository.softDeleteById(sectionId);
        log.info("Section with id {} deleted successfully", sectionId);
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
                .defaultRoom(entity.getDefaultRoom() == null
                        ? null
                        : new RoomBasicResponseDto(entity.getDefaultRoom().getUuid(), entity.getDefaultRoom().getName()))
                .classTeacherUuid(entity.getClassTeacher() == null ? null : entity.getClassTeacher().getUuid())
                .classTeacherName(entity.getClassTeacher() == null
                        ? null
                        : (entity.getClassTeacher().getUserProfile().getFirstName() + " " + entity.getClassTeacher().getUserProfile().getLastName()).trim())
                .build();
    }

    private Room resolveDefaultRoom(UUID defaultRoomId) {
        if (defaultRoomId == null) {
            return null;
        }
        return roomRepository.findActiveById(defaultRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Default room not found with id: " + defaultRoomId));
    }

    private Staff resolveClassTeacher(UUID classTeacherUuid) {
        if (classTeacherUuid == null) {
            return null;
        }
        return staffRepository.findByUuid(classTeacherUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Class teacher not found with id: " + classTeacherUuid));
    }

}