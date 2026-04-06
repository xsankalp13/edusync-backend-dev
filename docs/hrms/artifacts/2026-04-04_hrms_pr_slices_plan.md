# HRMS PR Slice Plan

Date: 2026-04-04

## Slice 1 — Critical Leave Governance (Role-Aware + Self-Approval Guard)

Goal:
- Complete role-aware leave listing behavior.
- Ensure leave review is tracked by reviewer user id/name.
- Block applicant self-approval.

Scope (key files):
- `src/main/java/com/project/edusync/hrms/controller/LeaveManagementController.java`
- `src/main/java/com/project/edusync/hrms/service/LeaveManagementService.java`
- `src/main/java/com/project/edusync/hrms/service/impl/LeaveManagementServiceImpl.java`
- `src/main/java/com/project/edusync/hrms/model/entity/LeaveApplication.java`
- `src/main/java/com/project/edusync/hrms/dto/leave/LeaveApplicationResponseDTO.java`
- `src/test/java/com/project/edusync/hrms/service/impl/LeaveManagementServiceImplTest.java`
- `src/test/java/com/project/edusync/hrms/service/impl/LeaveManagementLifecycleFlowTest.java`

Verification:
- `LeaveManagementServiceImplTest`
- `LeaveManagementLifecycleFlowTest`

---

## Slice 2 — Staff Category + Designation Foundation

Goal:
- Introduce category-aware staff model and designation master.
- Enforce designation/category consistency during staff create/update.
- Expose designation CRUD APIs and admin listing filters.

Scope (key files):
- `src/main/java/com/project/edusync/uis/model/enums/StaffCategory.java`
- `src/main/java/com/project/edusync/hrms/model/entity/StaffDesignation.java`
- `src/main/java/com/project/edusync/hrms/repository/StaffDesignationRepository.java`
- `src/main/java/com/project/edusync/hrms/service/StaffDesignationService.java`
- `src/main/java/com/project/edusync/hrms/service/impl/StaffDesignationServiceImpl.java`
- `src/main/java/com/project/edusync/hrms/controller/StaffDesignationController.java`
- `src/main/java/com/project/edusync/uis/model/entity/Staff.java`
- `src/main/java/com/project/edusync/iam/service/impl/UserManagementServiceImpl.java`
- `src/main/java/com/project/edusync/uis/repository/StaffRepository.java`
- `src/main/java/com/project/edusync/uis/service/impl/AdminUserQueryServiceImpl.java`
- `src/main/java/com/project/edusync/uis/controller/AdminUserQueryController.java`
- `src/test/java/com/project/edusync/hrms/service/impl/StaffDesignationServiceImplTest.java`
- `src/test/java/com/project/edusync/hrms/controller/StaffDesignationControllerSecurityTest.java`
- `src/test/java/com/project/edusync/iam/service/impl/UserManagementServiceImplFlowTest.java`

Verification:
- `StaffDesignationServiceImplTest`
- `StaffDesignationControllerSecurityTest`
- `UserManagementServiceImplFlowTest`

---

## Slice 3 — Category-Aware Leave/Salary Masters + Migration + FE Handoff

Goal:
- Add category applicability for leave types.
- Add optional category filtering for salary templates.
- Deliver migration script and frontend handoff updates.

Scope (key files):
- `src/main/java/com/project/edusync/hrms/model/entity/LeaveTypeConfig.java`
- `src/main/java/com/project/edusync/hrms/repository/LeaveTypeConfigRepository.java`
- `src/main/java/com/project/edusync/hrms/service/impl/LeaveTypeConfigServiceImpl.java`
- `src/main/java/com/project/edusync/hrms/controller/LeaveTypeConfigController.java`
- `src/main/java/com/project/edusync/hrms/model/entity/SalaryTemplate.java`
- `src/main/java/com/project/edusync/hrms/repository/SalaryTemplateRepository.java`
- `src/main/java/com/project/edusync/hrms/service/impl/SalaryTemplateServiceImpl.java`
- `src/main/java/com/project/edusync/hrms/controller/SalaryTemplateController.java`
- `src/main/resources/db/migration/V20260404_04__staff_category_designation_and_leave_review_enhancements.sql`
- `docs/hrms/FRONTEND_IMPLEMENTATION_HANDOFF_2026-04-04.md`

Verification:
- `LeaveTypeConfigServiceImplTest`
- `SalaryTemplateServiceImplTest`
- Flyway dry-run/preflight in target environment

