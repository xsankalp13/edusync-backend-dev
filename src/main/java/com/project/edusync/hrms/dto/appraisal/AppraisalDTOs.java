package com.project.edusync.hrms.dto.appraisal;

import com.project.edusync.hrms.model.enums.AppraisalCycleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AppraisalDTOs {

    public record CycleCreateDTO(@NotBlank String cycleName, String academicYear,
                                  @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}

    public record CycleResponseDTO(UUID uuid, String cycleName, String academicYear,
                                    LocalDate startDate, LocalDate endDate,
                                    AppraisalCycleStatus status, LocalDateTime createdAt) {}

    public record CycleStatusUpdateDTO(@NotNull AppraisalCycleStatus status) {}

    public record GoalCreateDTO(@NotBlank String staffRef, @NotBlank String goalTitle,
                                 String description, int weightage, String targetMetric) {}

    public record GoalResponseDTO(Long id, UUID uuid, UUID staffRef, String staffName,
                                   String goalTitle, String description, int weightage, String targetMetric) {}

    public record SelfReviewCreateDTO(@NotBlank String staffRef, Integer selfRating,
                                       String achievements, String challenges, String trainingNeeds) {}

    public record SelfReviewResponseDTO(UUID uuid, UUID staffRef, String staffName, Integer selfRating,
                                         String achievements, String challenges, String trainingNeeds,
                                         LocalDateTime submittedAt) {}

    public record ManagerReviewCreateDTO(@NotBlank String staffRef, String reviewerStaffRef,
                                          Integer managerRating, String strengths,
                                          String areasOfImprovement, String overallRemarks) {}

    public record ManagerReviewResponseDTO(UUID uuid, UUID staffRef, String staffName, Integer managerRating,
                                            String strengths, String areasOfImprovement,
                                            String overallRemarks, LocalDateTime submittedAt) {}

    public record CycleDetailDTO(CycleResponseDTO cycle, List<GoalResponseDTO> goals,
                                  List<SelfReviewResponseDTO> selfReviews,
                                  List<ManagerReviewResponseDTO> managerReviews) {}
}

