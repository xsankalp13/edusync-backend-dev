# HRMS Frontend Implementation Guide (Backend v2.0)

Date: 2026-04-04  
Audience: Frontend Team (Admin Portal, Principal/Admin Console, Staff Self-Service)

---

## 1) Purpose and How to Use This Document

This document is the frontend source-of-truth for integrating with the current HRMS backend.

It explains, in implementation detail:
- endpoint contracts and filters,
- role-based UX behavior,
- field-level request/response expectations,
- state transitions and action guards,
- frontend validation and error handling strategy,
- test cases and rollout checklist.

Use this as:
1. API integration guide,
2. UX behavior guide,
3. QA acceptance reference.

---

## 2) Executive Snapshot

### 2.1 Implemented and frontend-usable

- Academic calendar management
- Leave type configuration (now category-aware)
- Leave application lifecycle and balances (role-aware listing)
- Staff grade definitions + grade assignment history
- Staff designations (new master CRUD)
- Salary components/templates/mappings
- Payroll run lifecycle (`PROCESSED -> APPROVED -> DISBURSED`)
- Payslip list/detail/PDF (admin and self-service)
- HRMS dashboard summary

### 2.2 Notable recent backend changes FE must account for

- Staff taxonomy introduced:
  - `TEACHING`
  - `NON_TEACHING_ADMIN`
  - `NON_TEACHING_SUPPORT`
- New designation master: `/auth/hrms/designations`
- Leave applications now include:
  - `employeeId`
  - `staffCategory`
  - `designationName`
  - `reviewedByUserId`
  - `reviewedByName`
  - `reviewedAt`
- `GET /auth/hrms/leaves/applications` is role-aware:
  - admin roles see all
  - non-admin staff sees own only
- Leave types support optional category applicability filters.
- Salary templates support optional category filter.

---

## 3) Base URL, Auth, and Common Patterns

### 3.1 Base URL

All HRMS routes are under:
- `${api.url}/auth/hrms/...`

Where:
- `api.url = ${BASE_URL}${API_VERSION}`

Example:
- `BASE_URL=/api`
- `API_VERSION=/v1`
- Effective: `/api/v1`

### 3.2 Auth model

- Bearer token required.
- Backend enforces role checks.
- FE must also hide disallowed actions to reduce noisy `403` calls.

### 3.3 Pagination model

Pageable endpoints return Spring `Page<T>` shape:
- `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`, `empty`, `sort`

### 3.4 Date/time conventions

- Date input/output: `YYYY-MM-DD`
- Timestamp output: ISO datetime

---

## 4) Roles and UX Visibility Matrix

| Capability | SUPER_ADMIN | SCHOOL_ADMIN | ADMIN | PRINCIPAL | TEACHER/LIBRARIAN |
| --- | --- | --- | --- | --- | --- |
| Calendar CRUD | Yes | Yes | Yes | No | No |
| Leave Type CRUD | Yes | Yes | Yes | No | No |
| Designation CRUD list/create/update | Yes | Yes | Yes | No | No |
| Designation delete | Yes | No | No | No | No |
| Leave application list (all) | Yes | Yes | Yes | No | No |
| Leave application list (own) | Yes | Yes | Yes | Yes | Yes |
| Leave approve/reject | Yes | Yes | Yes | Yes | No |
| Leave apply/cancel own | Yes* | Yes* | Yes* | Yes | Yes |
| Grade/Assignment admin ops | Yes | Yes | Yes | No | No |
| Salary master/mappings | Yes | Yes | Yes | No | No |
| Payroll runs | Yes | Yes | Yes | No | No |
| Payslip admin access | Yes | Yes | Yes | No | No |
| Self payslip access | Yes* | Yes* | Yes* | Yes | Yes |

`*` admin users still need linked staff profile for staff-self endpoints (`leave apply`, `balance/me`) where backend expects staff context.

---

## 5) Domain Data Model FE Should Assume

### 5.1 Staff category enum

```ts
export type StaffCategory =
  | 'TEACHING'
  | 'NON_TEACHING_ADMIN'
  | 'NON_TEACHING_SUPPORT';
```

### 5.2 Leave status enum

```ts
export type LeaveApplicationStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED';
```

### 5.3 Payroll status enum

```ts
export type PayrollRunStatus =
  | 'PROCESSED'
  | 'APPROVED'
  | 'DISBURSED';
```

---

## 6) Endpoint-by-Endpoint Frontend Contract

## 6.1 Calendar

Controller: `AcademicCalendarController`  
Base: `${api.url}/auth/hrms/calendar`

Endpoints:
- `GET /events?academicYear=&month=`
- `POST /events`
- `POST /events/bulk`
- `PUT /events/{eventId}`
- `DELETE /events/{eventId}`
- `GET /summary?academicYear=`

FE guidance:
- Use summary endpoint for cards/charts.
- Keep calendar form strict on date/day-type validity.

---

## 6.2 Leave Type Configuration

Controller: `LeaveTypeConfigController`  
Base: `${api.url}/auth/hrms/leaves/types`

Endpoints:
- `GET /?category=TEACHING|NON_TEACHING_ADMIN|NON_TEACHING_SUPPORT`
- `GET /{leaveTypeId}`
- `POST /`
- `PUT /{leaveTypeId}`
- `DELETE /{leaveTypeId}` (soft delete)

Important payload behavior:
- `applicableCategories` empty or omitted = applicable to all categories.
- `requiresDocument=true` requires `documentRequiredAfterDays`.

Response fields to use:
- `sortOrder` for deterministic UI ordering
- `active` for soft-delete aware filtering
- `createdAt`, `updatedAt` for audit display if needed

FE form tips:
- Show multiselect for applicable categories.
- If `requiresDocument` toggled off, clear `documentRequiredAfterDays` in UI state.

---

## 6.3 Leave Management

Controller: `LeaveManagementController`  
Base: `${api.url}/auth/hrms/leaves`

### Applications

- `GET /applications`
  - filters: `staffId`, `status`, `leaveTypeCode`, `fromDate`, `toDate`, pageable
  - role-aware visibility:
    - admin roles: all (respecting filters)
    - non-admin users: own only (backend-enforced)
- `GET /applications/{applicationId}`
- `POST /applications`
- `POST /applications/{applicationId}/approve`
- `POST /applications/{applicationId}/reject`
- `POST /applications/{applicationId}/cancel`

### Balances

- `GET /balance/me`
- `GET /balance/{staffId}`
- `GET /balance/all` (pageable)
- `POST /balance/initialize`

Balance response shape clarification:
- `GET /balance/me` and `GET /balance/{staffId}` return `LeaveBalanceResponseDTO[]` (flat list, one item per leave type).
- `GET /balance/all` returns `Page<LeaveBalanceResponseDTO>`.

### Leave application response fields FE must map

```ts
export interface LeaveApplicationResponse {
  applicationId: number;
  uuid: string | null;
  staffId: number;
  staffName: string;
  employeeId: string | null;
  staffCategory: StaffCategory | null;
  designationName: string | null;

  leaveTypeId: number;
  leaveTypeCode: string;
  leaveTypeName: string;

  fromDate: string;
  toDate: string;
  totalDays: number;
  isHalfDay: boolean;
  halfDayType: 'FIRST_HALF' | 'SECOND_HALF' | null;

  reason: string;
  attachmentUrl: string | null;
  status: LeaveApplicationStatus;
  appliedOn: string;

  reviewedByUserId: number | null;
  reviewedByName: string | null;
  reviewRemarks: string | null;
  reviewedAt: string | null;
}
```

### Action-button visibility rules

- `Approve/Reject` button:
  - show only for privileged roles
  - show only when `status === 'PENDING'`
- `Cancel` button:
  - show for applicant/allowed roles
  - hide if already `REJECTED`/`CANCELLED`

### Critical edge behavior

- Backend rejects self-approval; FE should pre-disable if reviewer equals applicant when data available.
- Admin with no staff profile can approve/reject but cannot use staff-self endpoints.

---

## 6.4 Staff Designations (New Master)

Controller: `StaffDesignationController`  
Base: `${api.url}/auth/hrms/designations`

Endpoints:
- `GET /?category=&active=`
- `GET /{designationId}`
- `POST /`
- `PUT /{designationId}`
- `DELETE /{designationId}` (`SUPER_ADMIN` only)

Request DTO:

```json
{
  "designationCode": "PRT",
  "designationName": "Primary Teacher",
  "category": "TEACHING",
  "description": "...",
  "sortOrder": 10
}
```

Response DTO:

```json
{
  "designationId": 1,
  "uuid": "...",
  "designationCode": "PRT",
  "designationName": "Primary Teacher",
  "category": "TEACHING",
  "description": "...",
  "sortOrder": 10,
  "active": true,
  "createdAt": "...",
  "updatedAt": "..."
}
```

FE integration guidance:
- For staff forms:
  1. user selects `category`,
  2. fetch designations filtered by category,
  3. reset selected designation if category changes.
- Block delete button for non-`SUPER_ADMIN` roles.

---

## 6.5 Staff Grades and Grade Assignments

Controllers:
- `StaffGradeController`
- `StaffGradeAssignmentController`

Base: `${api.url}/auth/hrms/grades`

Endpoints:
- Grade master:
  - `GET /`
  - `POST /`
  - `PUT /{gradeId}`
  - `DELETE /{gradeId}`
- Assignment/history:
  - `POST /assign`
  - `GET /staff/{staffId}/current`
  - `GET /staff/{staffId}/history`
  - `GET /assignments`

FE behavior:
- Treat grade definitions and grade assignment history as separate tabs.
- Show assignment controls only for teaching category workflows.

---

## 6.6 Salary Components, Templates, and Staff Mapping

Controllers:
- `SalaryComponentController`
- `SalaryTemplateController`
- `StaffSalaryMappingController`

### Salary components
Base: `${api.url}/auth/hrms/salary/components`
- `GET /`, `POST /`, `PUT /{componentId}`, `DELETE /{componentId}`

### Salary templates
Base: `${api.url}/auth/hrms/salary/templates`
- `GET /?category=` (optional)
- `GET /{templateId}`
- `POST /`
- `PUT /{templateId}`
- `DELETE /{templateId}`

Template response now includes optional `applicableCategory`.

### Staff salary mappings
Base: `${api.url}/auth/hrms/salary/mappings`
- `GET /` pageable
- `GET /staff/{staffId}`
- `POST /`
- `PUT /{mappingId}`
- `POST /bulk`
- `GET /{mappingId}/computed`

FE behavior:
- Use `computed` endpoint as pre-submit preview source.
- Never recalculate statutory/derived values client-side.

---

## 6.7 Payroll Runs

Controller: `PayrollController`  
Base: `${api.url}/auth/hrms/payroll/runs`

Endpoints:
- `POST /`
- `POST /{runId}/approve`
- `POST /{runId}/disburse`
- `GET /`
- `GET /{runId}`

State guard UI:
- Approve button only when status = `PROCESSED`
- Disburse button only when status = `APPROVED`

---

## 6.8 Payslips

Controller: `PayrollPayslipController`  
Base: `${api.url}/auth/hrms/payroll`

Admin:
- `GET /runs/{runId}/payslips`
- `GET /payslips/{payslipId}`
- `GET /payslips/{payslipId}/pdf`
- `GET /staff/{staffId}/payslips`

Self:
- `GET /self/payslips`
- `GET /self/payslips/{payslipId}`
- `GET /self/payslips/{payslipId}/pdf`

PDF handling:
- request as `blob`/`arraybuffer`
- open object URL in tab or embedded viewer

---

## 6.9 Dashboard

Controller: `HrmsDashboardController`  
Base: `${api.url}/auth/hrms/dashboard`

Endpoint:
- `GET /summary`

Summary includes:
- overall counts (`totalActiveStaff`, mapping stats, leave/payroll totals)
- attendance totals (`todayPresent`, `todayAbsent`, `todayOnLeave`)
- category totals (`totalTeachingStaff`, `totalNonTeachingAdmin`, `totalNonTeachingSupport`)
- category attendance breakdown (`categoryAttendance[]` with `category`, `present`, `absent`, `onLeave`)
- grade and payroll trend arrays

Use for:
- top-level cards,
- leave/payout trend widgets,
- quick staffing indicators.

---

## 7) FE Validation Rules and Form Contracts

### 7.1 Leave apply form

Mandatory:
- `leaveTypeId`, `fromDate`, `toDate`, `reason`

Conditional:
- if `isHalfDay=true`:
  - `fromDate === toDate`
  - `halfDayType` required

Optional:
- `attachmentUrl`

### 7.2 Designation form

Mandatory:
- `designationCode`, `designationName`, `category`

Optional:
- `description`, `sortOrder`

Client checks:
- uppercase code display normalization (backend also normalizes)
- max lengths should mirror backend constraints

### 7.3 Salary template form

Mandatory:
- `templateName`, `academicYear`, `components[]`

Optional:
- `gradeId`, `description`, `applicableCategory`

Component validation:
- no duplicate component IDs in same template payload

---

## 8) Error Handling Contract

Global error shape (`ErrorResponse`):

```json
{
  "statusCode": 403,
  "message": "Access Denied: You do not have permission to perform this action.",
  "path": "/api/v1/auth/hrms/...",
  "timestamp": "2026-04-04T12:53:31.000Z"
}
```

Validation error shape (`ValidationErrorResponse`):

```json
{
  "statusCode": 400,
  "message": "Validation failed. Please check your input.",
  "path": "/api/v1/auth/hrms/...",
  "timestamp": "2026-04-04T12:53:31.000Z",
  "fieldErrors": {
    "fieldName": "error message"
  }
}
```

FE handling standards:
- show `message` toast/banner,
- bind `fieldErrors` into form inline errors,
- expected statuses: `400`, `401`, `403`, `404`, `405`, `409`, `500`.

---

## 9) Frontend Architecture Recommendations

### 9.1 Suggested API client module split

- `hrmsCalendarApi`
- `hrmsLeaveTypeApi`
- `hrmsLeaveApi`
- `hrmsDesignationApi`
- `hrmsGradeApi`
- `hrmsSalaryApi`
- `hrmsPayrollApi`
- `hrmsPayslipApi`
- `hrmsDashboardApi`

### 9.2 Query key suggestions (React Query style)

- `['hrms', 'leave', 'applications', filters]`
- `['hrms', 'leave', 'types', category]`
- `['hrms', 'designations', category, active]`
- `['hrms', 'salary', 'templates', category]`
- `['hrms', 'payroll', 'runs', pageable]`

### 9.3 Cache invalidation guidelines

Invalidate after mutating:
- leave approve/reject/cancel: application list + relevant balances
- designation create/update/delete: designations list
- salary template CRUD: templates list
- payroll transitions: run list + run detail + payslip lists

---

## 10) Screen-by-Screen FE Checklist

### 10.1 Leave admin queue

- filters: staff, status, leave type, date range
- columns: include category/designation
- actions: approve/reject with remarks modal

### 10.2 Staff leave self-service

- own application list
- apply form with half-day logic
- cancel action visibility by status

### 10.3 Designation master screen

- list with category + active filters
- create/edit modal
- super-admin-only delete action

### 10.4 Salary template screen

- category filter dropdown
- template form includes optional category
- computed preview before mapping confirmation

### 10.5 Payroll run screen

- state-aware action buttons
- clear status badges and transition history

### 10.6 Payslip screen

- admin and self tabs/routes
- PDF open/download handling with retry fallback

---

## 11) QA Scenarios (Frontend + Integration)

### 11.1 Leave scenarios

- admin sees all applications; teacher sees own only
- self-approval blocked (principal/admin edge)
- non-applicable leave type for category is rejected
- half-day invalid combinations produce form/server errors

### 11.2 Designation scenarios

- create designation and filter by category
- create staff with matching designation category works
- mismatched category/designation blocked
- delete blocked when designation in use

### 11.3 Salary scenarios

- templates filter by category works
- templates with null category still shown for all
- duplicate component in payload rejected

### 11.4 Payroll/payslip scenarios

- correct action-button state by run status
- disallowed transitions return proper error and UI handles gracefully
- payslip PDF open both admin and self paths

---

## 12) Migration and Rollout Coordination

Backend operational references:
- `docs/hrms/HRMS_MIGRATION_RUNBOOK.md`
- `docs/hrms/FLYWAY_ROLLOUT_CHECKLIST.md`
- `scripts/hrms_migration_preflight.sh`
- `scripts/hrms_regression_suite.sh`
- `docs/hrms/artifacts/2026-04-04_hrms_regression_baseline.md`
- `docs/hrms/artifacts/2026-04-04_hrms_pr_slices_plan.md`

FE rollout recommendations:
- run FE UAT after migration preflight passes,
- avoid release windows overlapping DB migration windows,
- smoke test role-based pages with at least one user per role profile.

---

## 13) Production-Ready FE Action Checklist

- [ ] Regenerate FE API types from OpenAPI.
- [ ] Add `StaffCategory` enum in FE shared model.
- [ ] Implement designation master pages and dropdown integration.
- [ ] Implement leave list role-aware UX assumptions (backend still enforces).
- [ ] Map new leave response reviewer + taxonomy fields.
- [ ] Add salary template category filtering and form support.
- [ ] Ensure payroll transitions are state-guarded in UI.
- [ ] Validate PDF blob handling across browsers.
- [ ] Final QA pass against scenarios in section 11.

---

## 14) Appendix: Reference Paths

- `src/main/java/com/project/edusync/hrms/controller/LeaveTypeConfigController.java`
- `src/main/java/com/project/edusync/hrms/controller/LeaveManagementController.java`
- `src/main/java/com/project/edusync/hrms/controller/StaffDesignationController.java`
- `src/main/java/com/project/edusync/hrms/controller/SalaryTemplateController.java`
- `src/main/java/com/project/edusync/hrms/service/impl/LeaveManagementServiceImpl.java`
- `src/main/java/com/project/edusync/hrms/service/impl/LeaveTypeConfigServiceImpl.java`
- `src/main/java/com/project/edusync/hrms/service/impl/SalaryTemplateServiceImpl.java`
- `src/main/java/com/project/edusync/iam/service/impl/UserManagementServiceImpl.java`
- `src/main/resources/db/migration/V20260404_04__staff_category_designation_and_leave_review_enhancements.sql`
- `docs/hrms/artifacts/2026-04-04_hrms_regression_baseline.md`
- `docs/hrms/artifacts/2026-04-04_hrms_pr_slices_plan.md`
