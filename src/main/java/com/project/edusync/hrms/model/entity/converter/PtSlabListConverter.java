package com.project.edusync.hrms.model.entity.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.hrms.dto.statutory.PtSlabDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Converter
public class PtSlabListConverter implements AttributeConverter<List<PtSlabDTO>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<PtSlabDTO> slabs) {
        if (slabs == null || slabs.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(slabs);
        } catch (Exception e) {
            log.error("Error serializing PT slabs", e);
            return null;
        }
    }

    @Override
    public List<PtSlabDTO> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Error deserializing PT slabs", e);
            return List.of();
        }
    }
}

