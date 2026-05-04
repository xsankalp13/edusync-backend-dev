package com.project.edusync.em.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalCapacityInfoDTO {
    private int maxStudentsInAnyTimeslot;
    private int totalStudentsAcrossExam;
    private int timeslotCount;
    private Map<String, Integer> studentsPerTimeslot;
}
