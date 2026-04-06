package com.project.edusync.ams.model.service.implementation;

import com.project.edusync.ams.model.dto.response.DailyRosterResponseDTO;
import com.project.edusync.ams.model.enums.ApprovalStatus;
import com.project.edusync.ams.model.repository.AbsenceDocumentationRepository;
import com.project.edusync.ams.model.service.DailyRosterService;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyRosterServiceImpl implements DailyRosterService {

    private final StudentRepository studentRepository;
    private final AbsenceDocumentationRepository absenceDocumentationRepository;

    @Override
    public List<DailyRosterResponseDTO> getDailyRoster(Long classId, LocalDate date) {

        // Using the newly added repository method
        return studentRepository.findByAcademicClassId(classId).stream()
                .map(student -> {
                    // Check if an APPROVED leave request exists for this student on this date
                    boolean hasApprovedLeave = absenceDocumentationRepository
                            .existsByStudentIdAndDateAndApprovalStatus(
                                    student.getId(),
                                    date,
                                    ApprovalStatus.APPROVED
                            );

                    // Map the name securely. Adjust getProfile() based on your actual Student entity.
                    // If Student inherits from User, it might be student.getUserProfile().getFirstName()
                    String fullName = "Student Name";
                    if (student.getUserProfile() != null) {
                        fullName = student.getUserProfile().getFirstName() + " " + student.getUserProfile().getLastName();
                    }

                    return new DailyRosterResponseDTO(student.getId(), fullName, hasApprovedLeave);
                })
                .collect(Collectors.toList());
    }
}