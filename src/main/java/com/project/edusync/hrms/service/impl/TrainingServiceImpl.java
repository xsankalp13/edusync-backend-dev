package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.training.TrainingDTOs.*;
import com.project.edusync.hrms.model.entity.*;
import com.project.edusync.hrms.model.enums.*;
import com.project.edusync.hrms.repository.*;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service("trainingService")
@RequiredArgsConstructor
public class TrainingServiceImpl {

    private final TrainingCourseRepository courseRepo;
    private final CourseEnrollmentRepository enrollmentRepo;
    private final CourseCertificateRepository certRepo;
    private final StaffRepository staffRepository;

    @Transactional
    public CourseResponseDTO createCourse(CourseCreateDTO dto) {
        TrainingCourse c = new TrainingCourse();
        c.setCourseCode(dto.courseCode()); c.setTitle(dto.title()); c.setDescription(dto.description());
        c.setFacilitator(dto.facilitator()); c.setStartDate(dto.startDate()); c.setEndDate(dto.endDate());
        c.setMaxSeats(dto.maxSeats()); c.setStatus(CourseStatus.UPCOMING);
        return toCourseResponse(courseRepo.save(c));
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDTO> listCourses() {
        return courseRepo.findAllByActiveTrue().stream().map(this::toCourseResponse).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponseDTO getCourse(UUID uuid) {
        return toCourseResponse(courseRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + uuid)));
    }

    @Transactional
    public CourseResponseDTO updateCourseStatus(UUID uuid, CourseStatus status) {
        TrainingCourse c = courseRepo.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        c.setStatus(status);
        return toCourseResponse(courseRepo.save(c));
    }

    @Transactional
    public EnrollmentResponseDTO enroll(UUID courseUuid, EnrollmentCreateDTO dto) {
        TrainingCourse c = courseRepo.findByUuid(courseUuid).orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        Staff staff = resolveStaff(dto.staffRef());
        if (enrollmentRepo.findByCourse_IdAndStaff_Id(c.getId(), staff.getId()).isPresent())
            throw new EdusyncException("Already enrolled", HttpStatus.CONFLICT);
        if (c.getMaxSeats() != null && enrollmentRepo.countByCourse_IdAndActiveTrue(c.getId()) >= c.getMaxSeats())
            throw new EdusyncException("Course is full", HttpStatus.BAD_REQUEST);
        CourseEnrollment e = new CourseEnrollment();
        e.setCourse(c); e.setStaff(staff); e.setEnrolledAt(LocalDateTime.now()); e.setStatus(EnrollmentStatus.ENROLLED);
        return toEnrollmentResponse(enrollmentRepo.save(e));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> listEnrollments(UUID courseUuid) {
        TrainingCourse c = courseRepo.findByUuid(courseUuid).orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return enrollmentRepo.findByCourse_Id(c.getId()).stream().map(this::toEnrollmentResponse).toList();
    }

    @Transactional
    public EnrollmentResponseDTO completeEnrollment(UUID courseUuid, Long enrollmentId, EnrollmentCompleteDTO dto) {
        CourseEnrollment e = findEnrollment(enrollmentId, courseUuid);
        e.setStatus(EnrollmentStatus.COMPLETED); e.setCompletedAt(LocalDateTime.now());
        if (dto.score() != null) e.setScore(dto.score());
        return toEnrollmentResponse(enrollmentRepo.save(e));
    }

    @Transactional
    public CertificateResponseDTO addCertificate(CertificateUploadDTO dto) {
        CourseEnrollment e = enrollmentRepo.findById(Long.parseLong(dto.enrollmentRef()))
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        CourseCertificate cert = certRepo.findByEnrollment_Id(e.getId()).orElse(new CourseCertificate());
        cert.setEnrollment(e); cert.setCertTitle(dto.certTitle()); cert.setIssuedAt(dto.issuedAt());
        cert.setExpiryDate(dto.expiryDate()); cert.setObjectKey(dto.objectKey()); cert.setStorageUrl(dto.storageUrl());
        return toCertResponse(certRepo.save(cert));
    }

    private CourseEnrollment findEnrollment(Long id, UUID courseUuid) {
        TrainingCourse c = courseRepo.findByUuid(courseUuid).orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return enrollmentRepo.findById(id).filter(e -> e.getCourse().getId().equals(c.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + id));
    }

    private Staff resolveStaff(String ref) { return PublicIdentifierResolver.resolve(ref, staffRepository::findByUuid, staffRepository::findById, "Staff"); }
    private String name(Staff s) { return s.getUserProfile() != null ? (s.getUserProfile().getFirstName() + " " + s.getUserProfile().getLastName()).trim() : ""; }
    private CourseResponseDTO toCourseResponse(TrainingCourse c) { return new CourseResponseDTO(c.getUuid(), c.getCourseCode(), c.getTitle(), c.getDescription(), c.getFacilitator(), c.getStartDate(), c.getEndDate(), c.getMaxSeats(), c.getStatus(), enrollmentRepo.countByCourse_IdAndActiveTrue(c.getId())); }
    private EnrollmentResponseDTO toEnrollmentResponse(CourseEnrollment e) { return new EnrollmentResponseDTO(e.getId(), e.getUuid(), e.getCourse().getUuid(), e.getCourse().getTitle(), e.getStaff().getUuid(), name(e.getStaff()), e.getEnrolledAt(), e.getCompletedAt(), e.getStatus(), e.getScore()); }
    private CertificateResponseDTO toCertResponse(CourseCertificate c) { return new CertificateResponseDTO(c.getUuid(), c.getEnrollment().getId(), c.getCertTitle(), c.getIssuedAt(), c.getExpiryDate(), c.getStorageUrl()); }
}

