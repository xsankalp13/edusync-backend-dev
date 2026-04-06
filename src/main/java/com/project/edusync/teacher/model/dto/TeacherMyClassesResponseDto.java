package com.project.edusync.teacher.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TeacherMyClassesResponseDto {
    private UUID classUuid;
    private String className;
    private UUID sectionUuid;
    private String sectionName;
    private boolean isClassTeacher;
    private List<SubjectItem> subjects;
    private long studentCount;

    @Data
    @Builder
    public static class SubjectItem {
        private UUID subjectUuid;
        private String subjectName;
        private String subjectCode;
    }
}

