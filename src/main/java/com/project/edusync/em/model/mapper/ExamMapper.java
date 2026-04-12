package com.project.edusync.em.model.mapper;
import com.project.edusync.em.model.dto.RequestDTO.ExamRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamResponseDTO;
import com.project.edusync.em.model.entity.Exam;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring", // Integrates with Spring as a Bean
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // Ignores fields that don't match
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ExamMapper {

    // Converts DTO -> Entity for creation
    Exam toEntity(ExamRequestDTO requestDTO);

    // Converts Entity -> DTO for responses
    @Mapping(target = "isPublished", source = "published") // Resolves boolean isPublished vs Boolean published mismatch
    @Mapping(target = "timetablePublished", source = "timetablePublished")
    @Mapping(target = "templateId", source = "template.uuid")
    @Mapping(target = "templateName", source = "template.name")
    ExamResponseDTO toResponseDTO(Exam exam);

    // Updates an existing entity from a DTO, ignoring nulls
    void updateEntityFromDto(ExamRequestDTO requestDTO, @MappingTarget Exam exam);
}