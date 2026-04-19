package com.project.edusync.discipline.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.discipline.model.dto.AdminRemarkDTO;
import com.project.edusync.discipline.model.dto.CreateRemarkRequest;
import com.project.edusync.discipline.model.dto.StudentRemarkDTO;
import com.project.edusync.discipline.model.entity.DisciplineRemark;
import com.project.edusync.discipline.model.enums.RemarkTag;
import com.project.edusync.discipline.repository.DisciplineRemarkRepository;
import com.project.edusync.discipline.service.DisciplineRemarkService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisciplineRemarkServiceImpl implements DisciplineRemarkService {

    private final DisciplineRemarkRepository remarkRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public StudentRemarkDTO createRemark(CreateRemarkRequest request, User teacherUser) {
        log.info("Creating discipline remark by user {} for student {}", teacherUser.getUsername(), request.getStudentUuid());

        // Resolve teacher (Staff) from logged-in user
        Staff teacher = staffRepository.findByUserProfile_User_Id(teacherUser.getId())
                .orElseThrow(() -> new EdusyncException("Your account is not linked to a staff profile.", HttpStatus.FORBIDDEN));

        // Resolve student by UUID
        Student student = studentRepository.findByUuid(request.getStudentUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", request.getStudentUuid()));

        DisciplineRemark remark = DisciplineRemark.builder()
                .student(student)
                .teacher(teacher)
                .message(request.getMessage())
                .tag(request.getTag())
                .remarkDate(request.getRemarkDate() != null ? request.getRemarkDate() : LocalDate.now())
                .build();

        remark = remarkRepository.save(remark);

        UserProfile teacherProfile = teacher.getUserProfile();
        return StudentRemarkDTO.builder()
                .uuid(remark.getUuid())
                .message(remark.getMessage())
                .tag(remark.getTag())
                .remarkDate(remark.getRemarkDate())
                .teacherName(teacherProfile.getFirstName() + " " + teacherProfile.getLastName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentRemarkDTO> getMyRemarks(User studentUser) {
        Student student = studentRepository.findByUserProfile_User_Id(studentUser.getId())
                .orElseThrow(() -> new EdusyncException("Your account is not linked to a student profile.", HttpStatus.FORBIDDEN));

        List<DisciplineRemark> remarks = remarkRepository.findByStudentIdWithTeacher(student.getId());
        return remarks.stream().map(this::toStudentRemarkDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentRemarkDTO> getTeacherRemarks(User teacherUser) {
        Staff teacher = staffRepository.findByUserProfile_User_Id(teacherUser.getId())
                .orElseThrow(() -> new EdusyncException("Your account is not linked to a staff profile.", HttpStatus.FORBIDDEN));

        List<DisciplineRemark> remarks = remarkRepository.findByTeacherIdWithDetails(teacher.getId());
        return remarks.stream().map(r -> {
            UserProfile tp = teacher.getUserProfile();
            return StudentRemarkDTO.builder()
                    .uuid(r.getUuid())
                    .message(r.getMessage())
                    .tag(r.getTag())
                    .remarkDate(r.getRemarkDate())
                    .teacherName(tp.getFirstName() + " " + tp.getLastName())
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminRemarkDTO> getAllRemarks(
            LocalDate fromDate,
            LocalDate toDate,
            UUID classUuid,
            UUID sectionUuid,
            RemarkTag tag,
            UUID teacherUuid,
            UUID studentUuid,
            String search,
            Pageable pageable
    ) {
        Specification<DisciplineRemark> spec = buildSpecification(fromDate, toDate, classUuid, sectionUuid, tag, teacherUuid, studentUuid, search);

        return remarkRepository.findAll(spec, pageable).map(this::toAdminRemarkDTO);
    }

    // --- Private helpers ---

    private Specification<DisciplineRemark> buildSpecification(
            LocalDate fromDate,
            LocalDate toDate,
            UUID classUuid,
            UUID sectionUuid,
            RemarkTag tag,
            UUID teacherUuid,
            UUID studentUuid,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Eager fetch joins for the main query (not count query)
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("student").fetch("userProfile");
                root.fetch("student").fetch("section").fetch("academicClass");
                root.fetch("teacher").fetch("userProfile");
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("remarkDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("remarkDate"), toDate));
            }
            if (tag != null) {
                predicates.add(cb.equal(root.get("tag"), tag));
            }
            if (classUuid != null) {
                Join<?, ?> studentJoin = root.join("student");
                Join<?, ?> sectionJoin = studentJoin.join("section");
                Join<?, ?> classJoin = sectionJoin.join("academicClass");
                predicates.add(cb.equal(classJoin.get("uuid"), classUuid));
            }
            if (sectionUuid != null) {
                Join<?, ?> studentJoin = root.join("student");
                Join<?, ?> sectionJoin = studentJoin.join("section");
                predicates.add(cb.equal(sectionJoin.get("uuid"), sectionUuid));
            }
            if (teacherUuid != null) {
                Join<?, ?> teacherJoin = root.join("teacher");
                predicates.add(cb.equal(teacherJoin.get("uuid"), teacherUuid));
            }
            if (studentUuid != null) {
                Join<?, ?> studentJoin = root.join("student");
                predicates.add(cb.equal(studentJoin.get("uuid"), studentUuid));
            }

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                
                Join<DisciplineRemark, Student> studentJoin = root.join("student");
                Join<Student, UserProfile> studentProfileJoin = studentJoin.join("userProfile");
                
                Join<DisciplineRemark, Staff> teacherJoin = root.join("teacher");
                Join<Staff, UserProfile> teacherProfileJoin = teacherJoin.join("userProfile");

                Predicate studentNamePred = cb.or(
                    cb.like(cb.lower(studentProfileJoin.get("firstName")), pattern),
                    cb.like(cb.lower(studentProfileJoin.get("lastName")), pattern)
                );
                
                Predicate teacherNamePred = cb.or(
                    cb.like(cb.lower(teacherProfileJoin.get("firstName")), pattern),
                    cb.like(cb.lower(teacherProfileJoin.get("lastName")), pattern)
                );

                Predicate enrollmentPred = cb.like(cb.lower(studentJoin.get("enrollmentNumber")), pattern);
                Predicate messagePred = cb.like(cb.lower(root.get("message")), pattern);

                predicates.add(cb.or(studentNamePred, teacherNamePred, enrollmentPred, messagePred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private StudentRemarkDTO toStudentRemarkDTO(DisciplineRemark r) {
        UserProfile tp = r.getTeacher().getUserProfile();
        return StudentRemarkDTO.builder()
                .uuid(r.getUuid())
                .message(r.getMessage())
                .tag(r.getTag())
                .remarkDate(r.getRemarkDate())
                .teacherName(tp.getFirstName() + " " + tp.getLastName())
                .build();
    }

    private AdminRemarkDTO toAdminRemarkDTO(DisciplineRemark r) {
        UserProfile sp = r.getStudent().getUserProfile();
        UserProfile tp = r.getTeacher().getUserProfile();
        return AdminRemarkDTO.builder()
                .uuid(r.getUuid())
                .message(r.getMessage())
                .tag(r.getTag())
                .remarkDate(r.getRemarkDate())
                .createdAt(r.getCreatedAt())
                .teacherName(tp.getFirstName() + " " + tp.getLastName())
                .teacherUuid(r.getTeacher().getUuid())
                .studentName(sp.getFirstName() + " " + sp.getLastName())
                .studentUuid(r.getStudent().getUuid())
                .enrollmentNumber(r.getStudent().getEnrollmentNumber())
                .className(r.getStudent().getSection().getAcademicClass().getName())
                .sectionName(r.getStudent().getSection().getSectionName())
                .build();
    }
}
