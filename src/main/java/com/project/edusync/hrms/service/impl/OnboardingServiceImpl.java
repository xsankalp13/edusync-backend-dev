package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.onboarding.OnboardingDTOs.*;
import com.project.edusync.hrms.model.entity.*;
import com.project.edusync.hrms.model.enums.OnboardingStatus;
import com.project.edusync.hrms.model.enums.TaskRecordStatus;
import com.project.edusync.hrms.repository.*;
import com.project.edusync.hrms.service.OnboardingService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final OnboardingTemplateRepository templateRepo;
    private final OnboardingTemplateTaskRepository templateTaskRepo;
    private final OnboardingRecordRepository recordRepo;
    private final OnboardingTaskRecordRepository taskRecordRepo;
    private final StaffRepository staffRepository;

    @Override @Transactional
    public TemplateResponseDTO createTemplate(TemplateCreateDTO dto) {
        OnboardingTemplate tmpl = new OnboardingTemplate();
        tmpl.setTemplateName(dto.templateName());
        tmpl.setDescription(dto.description());
        OnboardingTemplate saved = templateRepo.save(tmpl);
        saveTasks(saved, dto.tasks());
        return toTemplateResponse(saved);
    }

    @Override @Transactional
    public TemplateResponseDTO updateTemplate(UUID uuid, TemplateCreateDTO dto) {
        OnboardingTemplate tmpl = templateRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + uuid));
        tmpl.setTemplateName(dto.templateName());
        tmpl.setDescription(dto.description());
        List<OnboardingTemplateTask> existing = templateTaskRepo.findByTemplate_IdAndActiveTrueOrderByTaskOrderAsc(tmpl.getId());
        existing.forEach(t -> { t.setActive(false); templateTaskRepo.save(t); });
        OnboardingTemplate saved = templateRepo.save(tmpl);
        saveTasks(saved, dto.tasks());
        return toTemplateResponse(saved);
    }

    @Override @Transactional(readOnly = true)
    public List<TemplateResponseDTO> listTemplates() {
        return templateRepo.findAllByActiveTrue().stream().map(this::toTemplateResponse).toList();
    }

    @Override @Transactional
    public void deleteTemplate(UUID uuid) {
        OnboardingTemplate tmpl = templateRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + uuid));
        tmpl.setActive(false);
        templateRepo.save(tmpl);
    }

    @Override @Transactional
    public RecordResponseDTO createRecord(RecordCreateDTO dto) {
        Staff staff = resolveStaff(dto.staffRef());
        OnboardingTemplate tmpl = templateRepo.findByUuid(dto.templateRef())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + dto.templateRef()));

        OnboardingRecord record = new OnboardingRecord();
        record.setStaff(staff);
        record.setTemplate(tmpl);
        record.setStartDate(dto.startDate() != null ? dto.startDate() : LocalDate.now());
        record.setStatus(OnboardingStatus.IN_PROGRESS);
        OnboardingRecord saved = recordRepo.save(record);

        List<OnboardingTemplateTask> tasks = templateTaskRepo.findByTemplate_IdAndActiveTrueOrderByTaskOrderAsc(tmpl.getId());
        for (OnboardingTemplateTask task : tasks) {
            OnboardingTaskRecord tr = new OnboardingTaskRecord();
            tr.setRecord(saved);
            tr.setTemplateTask(task);
            tr.setDueDate(saved.getStartDate().plusDays(task.getDueAfterDays()));
            tr.setStatus(TaskRecordStatus.PENDING);
            taskRecordRepo.save(tr);
        }
        return toRecordResponse(saved);
    }

    @Override @Transactional(readOnly = true)
    public List<RecordResponseDTO> listRecords(String staffRef, OnboardingStatus status) {
        List<OnboardingRecord> records;
        if (staffRef != null) {
            Staff staff = resolveStaff(staffRef);
            records = status != null ? recordRepo.findByStaff_IdAndStatus(staff.getId(), status)
                    : recordRepo.findByStaff_Id(staff.getId());
        } else {
            records = status != null ? recordRepo.findByStatus(status) : recordRepo.findAllByActiveTrue();
        }
        return records.stream().map(this::toRecordResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public RecordResponseDTO getRecord(UUID uuid) {
        return toRecordResponse(recordRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding record not found: " + uuid)));
    }

    @Override @Transactional
    public RecordResponseDTO completeTask(UUID recordUuid, Long taskId, String actorRef, String remarks) {
        OnboardingRecord record = recordRepo.findByUuid(recordUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding record not found: " + recordUuid));
        OnboardingTaskRecord taskRecord = taskRecordRepo.findByRecord_IdAndId(record.getId(), taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        if (taskRecord.getStatus() == TaskRecordStatus.COMPLETED)
            throw new EdusyncException("Task already completed", HttpStatus.BAD_REQUEST);
        taskRecord.setStatus(TaskRecordStatus.COMPLETED);
        taskRecord.setCompletedAt(LocalDateTime.now());
        taskRecord.setRemarks(remarks);
        taskRecordRepo.save(taskRecord);

        long total = taskRecordRepo.countByRecord_Id(record.getId());
        long done = taskRecordRepo.countByRecord_IdAndStatus(record.getId(), TaskRecordStatus.COMPLETED);
        if (total > 0 && done == total) {
            record.setStatus(OnboardingStatus.COMPLETED);
            record.setCompletedAt(LocalDateTime.now());
            recordRepo.save(record);
        }
        return toRecordResponse(record);
    }

    @Override @Transactional(readOnly = true)
    public RecordResponseDTO getStaffOnboarding(String staffRef) {
        Staff staff = resolveStaff(staffRef);
        List<OnboardingRecord> records = recordRepo.findByStaff_Id(staff.getId());
        return records.stream()
                .filter(r -> r.getStatus() == OnboardingStatus.IN_PROGRESS)
                .findFirst()
                .or(() -> records.stream().findFirst())
                .map(this::toRecordResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No onboarding record for staff: " + staffRef));
    }

    private Staff resolveStaff(String ref) {
        return PublicIdentifierResolver.resolve(ref, staffRepository::findByUuid, staffRepository::findById, "Staff");
    }

    private void saveTasks(OnboardingTemplate tmpl, List<TemplateTaskDTO> tasks) {
        if (tasks == null) return;
        for (TemplateTaskDTO t : tasks) {
            OnboardingTemplateTask task = new OnboardingTemplateTask();
            task.setTemplate(tmpl);
            task.setTaskOrder(t.taskOrder());
            task.setTaskTitle(t.taskTitle());
            task.setDescription(t.description());
            task.setDueAfterDays(t.dueAfterDays());
            task.setAssignedParty(t.assignedParty());
            templateTaskRepo.save(task);
        }
    }

    private TemplateResponseDTO toTemplateResponse(OnboardingTemplate tmpl) {
        List<TemplateTaskDTO> tasks = templateTaskRepo.findByTemplate_IdAndActiveTrueOrderByTaskOrderAsc(tmpl.getId())
                .stream().map(t -> new TemplateTaskDTO(t.getId(), t.getUuid(), t.getTaskOrder(), t.getTaskTitle(),
                        t.getDescription(), t.getDueAfterDays(), t.getAssignedParty())).toList();
        return new TemplateResponseDTO(tmpl.getUuid(), tmpl.getTemplateName(), tmpl.getDescription(),
                tmpl.isActive(), tasks, tmpl.getCreatedAt());
    }

    private RecordResponseDTO toRecordResponse(OnboardingRecord record) {
        List<TaskRecordItemDTO> tasks = taskRecordRepo
                .findByRecord_IdOrderByTemplateTask_TaskOrderAsc(record.getId())
                .stream().map(t -> new TaskRecordItemDTO(t.getId(), t.getTemplateTask().getTaskTitle(),
                        t.getDueDate(), t.getStatus(), t.getTemplateTask().getAssignedParty(),
                        t.getCompletedAt(), t.getCompletedByName())).toList();
        long total = tasks.size();
        long done = tasks.stream().filter(t -> t.status() == TaskRecordStatus.COMPLETED).count();
        int pct = total == 0 ? 0 : (int) (done * 100 / total);
        Staff staff = record.getStaff();
        String name = staff.getUserProfile() != null
                ? (staff.getUserProfile().getFirstName() + " " + staff.getUserProfile().getLastName()).trim() : "";
        return new RecordResponseDTO(record.getUuid(), staff.getUuid(), name,
                record.getTemplate().getUuid(), record.getTemplate().getTemplateName(),
                record.getStartDate(), record.getTargetCompletionDate(), record.getStatus(),
                pct, record.getCompletedAt(), tasks);
    }
}

