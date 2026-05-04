package com.project.edusync.uis.service.impl;

import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.uis.model.dto.messaging.StudentMessageDTO;
import com.project.edusync.uis.model.dto.messaging.StudentMessageRequestDTO;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.messaging.StudentMessage;
import com.project.edusync.uis.repository.StudentMessageRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.GuardianRepository;
import com.project.edusync.uis.repository.StudentGuardianRelationshipRepository;
import com.project.edusync.uis.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunicationServiceImpl implements CommunicationService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StaffRepository staffRepository;
    private final StudentGuardianRelationshipRepository relationshipRepository;
    private final ScheduleRepository scheduleRepository;
    private final StudentMessageRepository studentMessageRepository;

    @Override
    @Transactional
    public StudentMessageDTO sendMessage(Long senderUserId, Long studentId, StudentMessageRequestDTO request) {
        User sender = userRepository.findByIdAsLong(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender user not found"));
        User receiver = userRepository.findByIdAsLong(request.getReceiverUserId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver user not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        // Determine roles
        var senderGuardianOpt = guardianRepository.findByUserProfile_User_Id(senderUserId);
        var senderStaffOpt = staffRepository.findByUserProfile_User_Id(senderUserId);

        // If sender is guardian -> receiver must be teacher of the student
        if (senderGuardianOpt.isPresent()) {
            Guardian guardian = senderGuardianOpt.get();
            // ensure guardian linked to student
            if (relationshipRepository.findByStudentAndGuardian(student, guardian).isEmpty()) {
                throw new IllegalArgumentException("Guardian not linked to student");
            }

            // check receiver is staff and teaches student or is class teacher
            var receiverStaffOpt = staffRepository.findByUserProfile_User_Id(receiver.getId());
            if (receiverStaffOpt.isEmpty()) {
                throw new IllegalArgumentException("Receiver is not staff/teacher");
            }
            Staff staff = receiverStaffOpt.get();
            boolean isTeacher = false;
            var schedules = scheduleRepository.findAllActiveByTeacherStaffIdAndSectionId(staff.getId(), student.getSection().getId());
            if (schedules != null && !schedules.isEmpty()) {
                isTeacher = true;
            } else if (student.getSection().getClassTeacher() != null && student.getSection().getClassTeacher().getId().equals(staff.getId())) {
                isTeacher = true;
            }
            if (!isTeacher) {
                throw new IllegalArgumentException("Teacher is not associated with this student");
            }
        } else if (senderStaffOpt.isPresent()) {
            // sender is staff/teacher -> receiver must be guardian of the student
            Staff staff = senderStaffOpt.get();
            var receiverGuardianOpt = guardianRepository.findByUserProfile_User_Id(receiver.getId());
            if (receiverGuardianOpt.isEmpty()) {
                throw new IllegalArgumentException("Receiver is not a guardian");
            }
            Guardian guardian = receiverGuardianOpt.get();
            if (relationshipRepository.findByStudentAndGuardian(student, guardian).isEmpty()) {
                throw new IllegalArgumentException("Guardian not linked to student");
            }

            // check that teacher teaches this student or is class teacher
            boolean isTeacher = false;
            var schedules = scheduleRepository.findAllActiveByTeacherStaffIdAndSectionId(staff.getId(), student.getSection().getId());
            if (schedules != null && !schedules.isEmpty()) {
                isTeacher = true;
            } else if (student.getSection().getClassTeacher() != null && student.getSection().getClassTeacher().getId().equals(staff.getId())) {
                isTeacher = true;
            }
            if (!isTeacher) {
                throw new IllegalArgumentException("Teacher not associated with this student");
            }
        } else {
            throw new IllegalArgumentException("Sender must be either a guardian or staff/teacher");
        }

        StudentMessage message = new StudentMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setStudent(student);
        message.setContent(request.getContent());
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);

        StudentMessage saved = studentMessageRepository.save(message);

        return new StudentMessageDTO(saved.getId(), saved.getSender().getId(), saved.getReceiver().getId(),
                saved.getStudent().getId(), saved.getContent(), saved.getSentAt(), saved.isRead());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentMessageDTO> getConversation(Long userId, Long studentId, Long otherUserId) {
        // Authorization: user must be guardian of student or teacher of student
        var guardianOpt = guardianRepository.findByUserProfile_User_Id(userId);
        var staffOpt = staffRepository.findByUserProfile_User_Id(userId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        boolean authorized = false;
        if (guardianOpt.isPresent()) {
            var rel = relationshipRepository.findByStudentAndGuardian(student, guardianOpt.get());
            authorized = rel.isPresent();
        }
        if (!authorized && staffOpt.isPresent()) {
            Staff staff = staffOpt.get();
            var schedules = scheduleRepository.findAllActiveByTeacherStaffIdAndSectionId(staff.getId(), student.getSection().getId());
            authorized = (schedules != null && !schedules.isEmpty()) || 
                         (student.getSection().getClassTeacher() != null && student.getSection().getClassTeacher().getId().equals(staff.getId()));
        }
        if (!authorized)
            throw new IllegalArgumentException("User not authorized to view messages for this student");

        List<com.project.edusync.uis.model.entity.messaging.StudentMessage> msgs = studentMessageRepository
                .findConversation(studentId, userId, otherUserId);

        return msgs.stream()
                .map(m -> new StudentMessageDTO(m.getId(), m.getSender().getId(), m.getReceiver().getId(),
                        m.getStudent().getId(), m.getContent(), m.getSentAt(), m.isRead()))
                .toList();
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long userId, Long studentId, Long otherUserId) {
        studentMessageRepository.markAsRead(studentId, userId, otherUserId);
    }
}
