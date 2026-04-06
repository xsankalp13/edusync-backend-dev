package com.project.edusync.teacher.service;

import com.project.edusync.teacher.model.entity.Incident;
import com.project.edusync.teacher.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public Incident logIncident(Incident incident) {
        return incidentRepository.save(incident);
    }

    public List<Incident> getIncidentsByStudent(Long studentId) {
        return incidentRepository.findByStudentId(studentId);
    }
}