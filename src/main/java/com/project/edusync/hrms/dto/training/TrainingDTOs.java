package com.project.edusync.hrms.dto.training;

import com.project.edusync.hrms.model.enums.CourseStatus;
import com.project.edusync.hrms.model.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class TrainingDTOs {

    public record CourseCreateDTO(@NotBlank String courseCode, @NotBlank String title, String description,
                                   String facilitator, LocalDate startDate, LocalDate endDate,
                                   Integer maxSeats) {}

    public record CourseResponseDTO(UUID uuid, String courseCode, String title, String description,
                                     String facilitator, LocalDate startDate, LocalDate endDate,
                                     Integer maxSeats, CourseStatus status, long enrolledCount) {}

    public record EnrollmentCreateDTO(@NotBlank String staffRef) {}

    public record EnrollmentResponseDTO(Long id, UUID uuid, UUID courseRef, String courseTitle,
                                         UUID staffRef, String staffName,
                                         LocalDateTime enrolledAt, LocalDateTime completedAt,
                                         EnrollmentStatus status, BigDecimal score) {}

    public record EnrollmentCompleteDTO(BigDecimal score) {}

    public record CertificateUploadDTO(@NotBlank String enrollmentRef, String certTitle,
                                        LocalDate issuedAt, LocalDate expiryDate,
                                        String objectKey, String storageUrl) {}

    public record CertificateResponseDTO(UUID uuid, Long enrollmentId, String certTitle,
                                          LocalDate issuedAt, LocalDate expiryDate, String storageUrl) {}
}

