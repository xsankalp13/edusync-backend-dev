package com.project.edusync.uis.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.uis.model.dto.messaging.StudentMessageDTO;
import com.project.edusync.uis.model.dto.messaging.StudentMessageRequestDTO;
import com.project.edusync.uis.service.CommunicationService;
import com.project.edusync.uis.model.dto.messaging.StudentContactDTO;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.repository.GuardianRepository;
import com.project.edusync.uis.repository.StudentGuardianRelationshipRepository;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.adm.repository.ScheduleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("${api.url}")
@RequiredArgsConstructor
@Validated
public class MessagingController {
    private final CommunicationService communicationService;
    private final AuthUtil authUtil;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRelationshipRepository relationshipRepository;
    private final StaffRepository staffRepository;
    private final ScheduleRepository scheduleRepository;
    private final com.project.edusync.uis.repository.StudentMessageRepository studentMessageRepository;

    @PostMapping("/messaging/student/{studentId}/messages")
    @PreAuthorize("hasAnyRole('GUARDIAN','TEACHER')")
    @Operation(summary = "Send message (teacher <-> guardian) regarding a student", security = @SecurityRequirement(name = "bearerAuth"))
    @Transactional
    public ResponseEntity<StudentMessageDTO> sendMessage(@PathVariable Long studentId,
            @Valid @RequestBody StudentMessageRequestDTO request) {
        Long userId = authUtil.getCurrentUserId();
        StudentMessageDTO dto = communicationService.sendMessage(userId, studentId, request);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/messaging/student/{studentId}/conversation/{otherUserId}")
    @PreAuthorize("hasAnyRole('GUARDIAN','TEACHER')")
    @Operation(summary = "Get conversation for a student between current user and other user", security = @SecurityRequirement(name = "bearerAuth"))
    @Transactional(readOnly = true)
    public ResponseEntity<List<StudentMessageDTO>> getConversation(@PathVariable Long studentId,
            @PathVariable Long otherUserId) {
        Long userId = authUtil.getCurrentUserId();
        List<StudentMessageDTO> conv = communicationService.getConversation(userId, studentId, otherUserId);
        return ResponseEntity.ok(conv);
    }

    @GetMapping("/messaging/student/{studentId}/teachers")
    @PreAuthorize("hasRole('GUARDIAN')")
    @Operation(summary = "Get list of teachers who teach the student's section (Guardian only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<StudentContactDTO>> getTeachersForStudent(@PathVariable Long studentId) {
        Long userId = authUtil.getCurrentUserId();

        var guardianOpt = guardianRepository.findByUserProfile_User_Id(userId);
        if (guardianOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        Guardian guardian = guardianOpt.get();

        var studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        Student student = studentOpt.get();

        var relOpt = relationshipRepository.findByStudentAndGuardian(student, guardian);
        if (relOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        var schedules = scheduleRepository.findAllActiveWithReferencesBySectionUuid(student.getSection().getUuid());
        var contactsList = new java.util.ArrayList<>(schedules.stream()
                .map(s -> s.getTeacher().getStaff())
                .filter(java.util.Objects::nonNull)
                .toList());

        if (student.getSection().getClassTeacher() != null && !contactsList.contains(student.getSection().getClassTeacher())) {
            contactsList.add(student.getSection().getClassTeacher());
        }

        var contacts = contactsList.stream()
                .distinct()
                .map(staff -> {
                    Long staffUserId = staff.getUserProfile() != null && staff.getUserProfile().getUser() != null 
                            ? staff.getUserProfile().getUser().getId() 
                            : null;
                    int unreadCount = 0;
                    if (staffUserId != null) {
                        unreadCount = studentMessageRepository.countByStudent_IdAndReceiver_IdAndSender_IdAndReadFalse(studentId, userId, staffUserId);
                    }
                    return new StudentContactDTO(
                            staffUserId,
                            staff.getUserProfile() != null
                                    ? staff.getUserProfile().getFirstName() + " " + staff.getUserProfile().getLastName()
                                    : null,
                            staff.getStaffType() != null ? staff.getStaffType().name() : "TEACHER",
                            unreadCount);
                })
                .toList();

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/messaging/student/{studentId}/guardians")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get list of guardians for a student (Teacher only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<StudentContactDTO>> getGuardiansForStudent(@PathVariable Long studentId) {
        Long userId = authUtil.getCurrentUserId();

        var staffOpt = staffRepository.findByUserProfile_User_Id(userId);
        if (staffOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        Staff staff = staffOpt.get();

        var studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        Student student = studentOpt.get();

        boolean isTeacher = false;
        var schedules = scheduleRepository.findAllActiveByTeacherStaffIdAndSectionId(staff.getId(),
                student.getSection().getId());
        if (schedules != null && !schedules.isEmpty()) {
            isTeacher = true;
        } else if (student.getSection().getClassTeacher() != null && student.getSection().getClassTeacher().getId().equals(staff.getId())) {
            isTeacher = true;
        }

        if (!isTeacher) {
            return ResponseEntity.status(404).build();
        }

        var relationships = relationshipRepository.findByStudent(student);
        var contacts = relationships.stream()
                .map(rel -> rel.getGuardian())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(g -> {
                    Long guardianUserId = g.getUserProfile() != null && g.getUserProfile().getUser() != null
                            ? g.getUserProfile().getUser().getId()
                            : null;
                    int unreadCount = 0;
                    if (guardianUserId != null) {
                        unreadCount = studentMessageRepository.countByStudent_IdAndReceiver_IdAndSender_IdAndReadFalse(studentId, userId, guardianUserId);
                    }
                    return new StudentContactDTO(
                            guardianUserId,
                            g.getUserProfile() != null
                                    ? g.getUserProfile().getFirstName() + " " + g.getUserProfile().getLastName()
                                    : null,
                            "GUARDIAN",
                            unreadCount);
                })
                .toList();

        return ResponseEntity.ok(contacts);
    }

    @PostMapping("/messaging/student/{studentId}/read/{otherUserId}")
    @PreAuthorize("hasAnyRole('GUARDIAN','TEACHER')")
    @Operation(summary = "Mark messages as read for a conversation", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> markAsRead(@PathVariable Long studentId, @PathVariable Long otherUserId) {
        Long userId = authUtil.getCurrentUserId();
        communicationService.markConversationAsRead(userId, studentId, otherUserId);
        return ResponseEntity.ok().build();
    }
}
