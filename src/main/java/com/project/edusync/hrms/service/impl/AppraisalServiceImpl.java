package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.appraisal.AppraisalDTOs.*;
import com.project.edusync.hrms.model.entity.*;
import com.project.edusync.hrms.model.enums.AppraisalCycleStatus;
import com.project.edusync.hrms.repository.*;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service("appraisalService")
@RequiredArgsConstructor
public class AppraisalServiceImpl {

    private final AppraisalCycleRepository cycleRepo;
    private final AppraisalGoalRepository goalRepo;
    private final SelfAppraisalReviewRepository selfRepo;
    private final ManagerAppraisalReviewRepository managerRepo;
    private final StaffRepository staffRepository;

    @Transactional
    public CycleResponseDTO createCycle(CycleCreateDTO dto) {
        AppraisalCycle c = new AppraisalCycle();
        c.setCycleName(dto.cycleName()); c.setAcademicYear(dto.academicYear());
        c.setStartDate(dto.startDate()); c.setEndDate(dto.endDate());
        c.setStatus(AppraisalCycleStatus.DRAFT);
        return toCycleResponse(cycleRepo.save(c));
    }

    @Transactional(readOnly = true)
    public List<CycleResponseDTO> listCycles() {
        return cycleRepo.findAllByActiveTrue().stream().map(this::toCycleResponse).toList();
    }

    @Transactional
    public CycleResponseDTO updateCycleStatus(UUID uuid, AppraisalCycleStatus status) {
        AppraisalCycle c = cycleRepo.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + uuid));
        c.setStatus(status);
        return toCycleResponse(cycleRepo.save(c));
    }

    @Transactional
    public GoalResponseDTO addGoal(UUID cycleUuid, GoalCreateDTO dto) {
        AppraisalCycle c = cycleRepo.findByUuid(cycleUuid).orElseThrow(() -> new ResourceNotFoundException("Cycle not found"));
        Staff staff = resolveStaff(dto.staffRef());
        AppraisalGoal g = new AppraisalGoal();
        g.setCycle(c); g.setStaff(staff); g.setGoalTitle(dto.goalTitle());
        g.setDescription(dto.description()); g.setWeightage(dto.weightage()); g.setTargetMetric(dto.targetMetric());
        return toGoalResponse(goalRepo.save(g));
    }

    @Transactional(readOnly = true)
    public List<GoalResponseDTO> listGoals(UUID cycleUuid, String staffRef) {
        AppraisalCycle c = cycleRepo.findByUuid(cycleUuid).orElseThrow(() -> new ResourceNotFoundException("Cycle not found"));
        List<AppraisalGoal> goals = staffRef != null
                ? goalRepo.findByCycle_IdAndStaff_Id(c.getId(), resolveStaff(staffRef).getId())
                : goalRepo.findByCycle_Id(c.getId());
        return goals.stream().map(this::toGoalResponse).toList();
    }

    @Transactional
    public SelfReviewResponseDTO submitSelfReview(UUID cycleUuid, SelfReviewCreateDTO dto) {
        AppraisalCycle c = cycleRepo.findByUuid(cycleUuid).orElseThrow(() -> new ResourceNotFoundException("Cycle not found"));
        Staff staff = resolveStaff(dto.staffRef());
        SelfAppraisalReview r = selfRepo.findByCycle_IdAndStaff_Id(c.getId(), staff.getId())
                .orElse(new SelfAppraisalReview());
        r.setCycle(c); r.setStaff(staff); r.setSelfRating(dto.selfRating());
        r.setAchievements(dto.achievements()); r.setChallenges(dto.challenges());
        r.setTrainingNeeds(dto.trainingNeeds()); r.setSubmittedAt(LocalDateTime.now());
        return toSelfResponse(selfRepo.save(r));
    }

    @Transactional
    public ManagerReviewResponseDTO submitManagerReview(UUID cycleUuid, ManagerReviewCreateDTO dto) {
        AppraisalCycle c = cycleRepo.findByUuid(cycleUuid).orElseThrow(() -> new ResourceNotFoundException("Cycle not found"));
        Staff staff = resolveStaff(dto.staffRef());
        ManagerAppraisalReview r = managerRepo.findByCycle_IdAndStaff_Id(c.getId(), staff.getId())
                .orElse(new ManagerAppraisalReview());
        r.setCycle(c); r.setStaff(staff);
        if (dto.reviewerStaffRef() != null) r.setReviewerStaff(resolveStaff(dto.reviewerStaffRef()));
        r.setManagerRating(dto.managerRating()); r.setStrengths(dto.strengths());
        r.setAreasOfImprovement(dto.areasOfImprovement()); r.setOverallRemarks(dto.overallRemarks());
        r.setSubmittedAt(LocalDateTime.now());
        return toManagerResponse(managerRepo.save(r));
    }

    @Transactional(readOnly = true)
    public CycleDetailDTO getCycleDetail(UUID uuid) {
        AppraisalCycle c = cycleRepo.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Cycle not found"));
        return new CycleDetailDTO(toCycleResponse(c),
                goalRepo.findByCycle_Id(c.getId()).stream().map(this::toGoalResponse).toList(),
                selfRepo.findByCycle_Id(c.getId()).stream().map(this::toSelfResponse).toList(),
                managerRepo.findByCycle_Id(c.getId()).stream().map(this::toManagerResponse).toList());
    }

    private Staff resolveStaff(String ref) {
        return PublicIdentifierResolver.resolve(ref, staffRepository::findByUuid, staffRepository::findById, "Staff");
    }
    private String name(Staff s) { return s.getUserProfile() != null ? (s.getUserProfile().getFirstName() + " " + s.getUserProfile().getLastName()).trim() : ""; }
    private CycleResponseDTO toCycleResponse(AppraisalCycle c) { return new CycleResponseDTO(c.getUuid(), c.getCycleName(), c.getAcademicYear(), c.getStartDate(), c.getEndDate(), c.getStatus(), c.getCreatedAt()); }
    private GoalResponseDTO toGoalResponse(AppraisalGoal g) { return new GoalResponseDTO(g.getId(), g.getUuid(), g.getStaff().getUuid(), name(g.getStaff()), g.getGoalTitle(), g.getDescription(), g.getWeightage(), g.getTargetMetric()); }
    private SelfReviewResponseDTO toSelfResponse(SelfAppraisalReview r) { return new SelfReviewResponseDTO(r.getUuid(), r.getStaff().getUuid(), name(r.getStaff()), r.getSelfRating(), r.getAchievements(), r.getChallenges(), r.getTrainingNeeds(), r.getSubmittedAt()); }
    private ManagerReviewResponseDTO toManagerResponse(ManagerAppraisalReview r) { return new ManagerReviewResponseDTO(r.getUuid(), r.getStaff().getUuid(), name(r.getStaff()), r.getManagerRating(), r.getStrengths(), r.getAreasOfImprovement(), r.getOverallRemarks(), r.getSubmittedAt()); }
}

