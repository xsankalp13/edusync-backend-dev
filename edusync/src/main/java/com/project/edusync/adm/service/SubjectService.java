package com.project.edusync.adm.service;

import com.project.edusync.adm.model.dto.request.SubjectRequestDto;
import com.project.edusync.adm.model.dto.response.SubjectResponseDto;

import java.util.List;
import java.util.UUID;

public interface SubjectService {

    SubjectResponseDto addSubject(SubjectRequestDto subjectRequestDto);

    List<SubjectResponseDto> getAllSubjects();

    SubjectResponseDto getSubjectById(UUID subjectId);

    SubjectResponseDto updateSubject(UUID subjectId, SubjectRequestDto subjectRequestDto);

    void deleteSubject(UUID subjectId);
}