package com.project.edusync.hrms.dto.onboarding;

import com.project.edusync.hrms.model.enums.AssignedParty;
import com.project.edusync.hrms.model.enums.OnboardingStatus;
import com.project.edusync.hrms.model.enums.TaskRecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OnboardingDTOs {

    public record TemplateTaskDTO(Long id, UUID uuid, int taskOrder, @NotBlank String taskTitle,
                                   String description, int dueAfterDays, AssignedParty assignedParty) {}

    public record TemplateCreateDTO(@NotBlank String templateName, String description,
                                     List<TemplateTaskDTO> tasks) {}

    public record TemplateResponseDTO(UUID uuid, String templateName, String description,
                                       boolean active, List<TemplateTaskDTO> tasks, LocalDateTime createdAt) {}

    public record RecordCreateDTO(@NotNull String staffRef, @NotNull UUID templateRef, LocalDate startDate) {}

    public record TaskRecordItemDTO(Long id, String taskTitle, LocalDate dueDate, TaskRecordStatus status,
                                     AssignedParty assignedParty, LocalDateTime completedAt, String completedByName) {}

    public record RecordResponseDTO(UUID uuid, UUID staffRef, String staffName, UUID templateRef, String templateName,
                                     LocalDate startDate, LocalDate targetCompletionDate,
                                     OnboardingStatus status, int completionPercentage,
                                     LocalDateTime completedAt, List<TaskRecordItemDTO> tasks) {}

    public record CompleteTaskDTO(String remarks) {}
}

