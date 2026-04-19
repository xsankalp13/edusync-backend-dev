package com.project.edusync.em.model.dto.internal.admitbatch;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentDTO {
    Long id;
    String name;
    Integer rollNo;
    String enrollmentNumber;
    String className;
    String sectionName;
    String photoBase64;
}

