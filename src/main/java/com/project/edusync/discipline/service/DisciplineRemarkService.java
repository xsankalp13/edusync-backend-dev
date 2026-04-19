package com.project.edusync.discipline.service;

import com.project.edusync.discipline.model.dto.AdminRemarkDTO;
import com.project.edusync.discipline.model.dto.CreateRemarkRequest;
import com.project.edusync.discipline.model.dto.StudentRemarkDTO;
import com.project.edusync.discipline.model.enums.RemarkTag;
import com.project.edusync.iam.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DisciplineRemarkService {

    /**
     * Create a new remark (called by a teacher).
     */
    StudentRemarkDTO createRemark(CreateRemarkRequest request, User teacherUser);

    /**
     * Get all remarks for the currently logged-in student.
     */
    List<StudentRemarkDTO> getMyRemarks(User studentUser);

    /**
     * Get all remarks created by the currently logged-in teacher.
     */
    List<StudentRemarkDTO> getTeacherRemarks(User teacherUser);

    /**
     * Admin: Get all remarks, with optional filters and pagination.
     */
    Page<AdminRemarkDTO> getAllRemarks(
            LocalDate fromDate,
            LocalDate toDate,
            UUID classUuid,
            UUID sectionUuid,
            RemarkTag tag,
            UUID teacherUuid,
            UUID studentUuid,
            String search,
            Pageable pageable
    );
}
