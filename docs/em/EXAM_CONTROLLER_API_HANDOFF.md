# Exam Controller API Handoff

## Purpose
This document is a frontend handoff for the Examination Controller role APIs and related business rules.

## Role and Access Model
- New role: `ROLE_EXAM_CONTROLLER`
- Access model is hybrid:
  - `ADMIN`, `SCHOOL_ADMIN`, `SUPER_ADMIN` have full exam-admin access.
  - `EXAM_CONTROLLER` can access`` only exams explicitly assigned to them.
- Scope checks are enforced by backend helper methods:
  - `@examControllerAccess.canAccessExam(examId)`
  - `@examControllerAccess.canAccessExamUuid(examUuid)`
  - `@examControllerAccess.canAccessSchedule(scheduleId)`
  - `@examControllerAccess.canAccessInvigilation(invigilationId)`

## New Endpoints

### 1) Assign Controller
- Method: `POST`
- URL: `/api/v1/auth/examination/exam-controller/assignments`
- Allowed roles: `ADMIN`, `SCHOOL_ADMIN`, `SUPER_ADMIN`

Request:
```json
{
  "examId": 12,
  "staffId": 45
}
```

Response:
```json
{
  "assignmentId": 9,
  "examId": 12,
  "staffId": 45,
  "staffName": "Ravi Sharma",
  "assignedByUserId": 3,
  "assignedAt": "2026-04-23T16:20:11"
}
```

### 2) Dashboard Summary
- Method: `GET`
- URL: `/api/v1/auth/examination/exam-controller/dashboard?examId=12`
- Allowed roles: assigned `EXAM_CONTROLLER` for this exam, or admin roles.

Response:
```json
{
  "examId": 12,
  "rooms": [
    {
      "roomId": 1,
      "roomName": "A1",
      "allocatedStudents": 25,
      "markedStudents": 10,
      "attendanceStatus": "IN_PROGRESS"
    }
  ],
  "timer": {
    "startTime": "2026-04-23T09:00:00",
    "endTime": "2026-04-23T12:00:00",
    "remainingSeconds": 4820
  }
}
```

### 3) Class View
- Method: `GET`
- URL: `/api/v1/auth/examination/exam-controller/dashboard/class-view?examId=12`
- Allowed roles: assigned `EXAM_CONTROLLER` for this exam, or admin roles.

Response shape:
```json
{
  "examId": 12,
  "classes": [
    {
      "className": "Class 10",
      "students": [
        {
          "studentId": 201,
          "studentName": "Anika Das",
          "rollNo": 14,
          "rooms": [
            {
              "roomId": 1,
              "roomName": "A1",
              "examScheduleId": 101,
              "subjectName": "Math",
              "seatNumber": "R2-C3",
              "attendanceStatus": "PRESENT",
              "entryAllowed": true
            }
          ]
        }
      ]
    }
  ]
}
```

### 4) Room View
- Method: `GET`
- URL: `/api/v1/auth/examination/exam-controller/dashboard/room-view?examId=12`
- Allowed roles: assigned `EXAM_CONTROLLER` for this exam, or admin roles.

Response shape:
```json
{
  "examId": 12,
  "rooms": [
    {
      "roomId": 1,
      "roomName": "A1",
      "students": [
        {
          "studentId": 201,
          "studentName": "Anika Das",
          "rollNo": 14,
          "className": "Class 10",
          "examScheduleId": 101,
          "subjectName": "Math",
          "seatNumber": "R2-C3",
          "attendanceStatus": "PRESENT",
          "entryAllowed": true
        }
      ]
    }
  ]
}
```

### 5) Defaulter Allow/Block Decision
- Method: `POST`
- URL: `/api/v1/auth/examination/exam-controller/defaulters/decision`
- Allowed roles: assigned `EXAM_CONTROLLER` for schedule's exam, or admin roles.

Request:
```json
{
  "examScheduleId": 101,
  "studentId": 201,
  "allowed": false,
  "reason": "Fee defaulter"
}
```

Response:
```json
{
  "examScheduleId": 101,
  "studentId": 201,
  "allowed": false,
  "reason": "Fee defaulter",
  "decidedByStaffId": 45,
  "decidedAt": "2026-04-23T16:35:00"
}
```

## Updated Existing Endpoints (Frontend Impact)

### Attendance Roster
- Endpoint: `GET /api/v1/auth/examination/exam-attendance/room/{roomId}?examScheduleId=...`
- Added field in each student row:
  - `entryAllowed: boolean`

### Mark Attendance
- Endpoint: `POST /api/v1/auth/examination/exam-attendance/mark`
- Rule update:
  - if `entryAllowed == false`, marking `PRESENT` returns validation error.

## Business Rules

### Data Source Rules
- Room dashboards, class view, and room view are driven by `seat_allocation`.
- Attendance display uses room-based joins from seat allocations plus attendance rows.
- No class-based attendance filtering logic is used for progress calculations.

### Attendance Progress Rules (Room-wise)
- Let:
  - `allocated = total allocated students in room for selected exam`
  - `marked = total attendance rows marked`
- Status mapping:
  - `marked == 0` -> `NOT_STARTED`
  - `0 < marked < allocated` -> `IN_PROGRESS`
  - `marked >= allocated` -> `COMPLETED`

### Timer Rules
- Backend derives windows from exam schedules (`examDate + timeslot.start/end`).
- Returns `startTime`, `endTime`, and `remainingSeconds`.
- `remainingSeconds = max(0, endTime - now)` in seconds.

### Defaulter Rules
- Entry decision scope is per `(examScheduleId, studentId)`.
- Default behavior when no record exists: `allowed = true`.
- If blocked (`allowed = false`), attendance cannot be marked as `PRESENT`.

## Recommended Frontend UI Mapping

### A) Assignment Screen (Admin)
- Add "Assign Controller" action in exam list/details.
- Inputs:
  - exam selector (or current exam)
  - staff selector
- Save via `POST /exam-controller/assignments`.

### B) Controller Dashboard
- Header:
  - exam name (from existing exam details API)
  - timer chip from dashboard timer object
- Room summary cards:
  - room name
  - allocated count
  - marked count
  - status badge (`NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`)

### C) Class Tab
- Group by class, expandable students.
- For each room record, show:
  - subject, room, seat number
  - attendance status
  - entryAllowed flag

### D) Room Tab
- Group by room.
- Show mixed-class student list.
- Columns:
  - roll no, student, class, subject, seat, attendance, entryAllowed

### E) Defaulter Toggle
- In class/room lists, add action:
  - Allow / Block
  - Optional reason text box
- Save via `POST /defaulters/decision`.

### F) Attendance Workflow
- Reuse existing attendance mark/finalize screens.
- Display `entryAllowed` badge.
- Prevent PRESENT selection for blocked students in UI as pre-check.

## Error Handling Notes
- Typical failure conditions:
  - `403` when controller is not assigned to exam scope.
  - `400` when trying to mark blocked student as PRESENT.
  - `404` for missing exam/schedule/student/staff references.
- Recommended UX:
  - show toast + inline row error for attendance mark rejection.

## Backend Files Added/Updated (for reference)
- Added:
  - `src/main/java/com/project/edusync/em/model/controller/ExamControllerManagementController.java`
  - `src/main/java/com/project/edusync/em/model/service/ExamControllerAccessService.java`
  - `src/main/java/com/project/edusync/em/model/service/ExamControllerAssignmentService.java`
  - `src/main/java/com/project/edusync/em/model/service/ExamControllerDashboardService.java`
  - `src/main/java/com/project/edusync/em/model/service/ExamEntryDecisionService.java`
  - `src/main/java/com/project/edusync/em/model/entity/ExamControllerAssignment.java`
  - `src/main/java/com/project/edusync/em/model/entity/ExamEntryDecision.java`
  - `src/main/java/com/project/edusync/em/model/repository/ExamControllerAssignmentRepository.java`
  - `src/main/java/com/project/edusync/em/model/repository/ExamEntryDecisionRepository.java`
  - `src/main/java/com/project/edusync/em/model/enums/RoomAttendanceProgressStatus.java`
  - `src/main/resources/db/migration/V20260423_01__exam_controller_assignment_and_entry_decision.sql`
- Updated (not exhaustive):
  - `src/main/java/com/project/edusync/common/config/DataSeeder.java`
  - `src/main/java/com/project/edusync/em/model/repository/SeatAllocationRepository.java`
  - `src/main/java/com/project/edusync/em/model/service/ExamAttendanceService.java`
  - `src/main/java/com/project/edusync/em/model/dto/response/ExamRoomStudentResponseDTO.java`
  - exam module controllers to include `EXAM_CONTROLLER` role and scoped checks.

## Quick Frontend Integration Checklist
- Add role-aware menu entry for Exam Controller Dashboard.
- Add assignment UI in admin exam page.
- Integrate dashboard API with polling/refresh option.
- Add class-view and room-view tabs using dedicated endpoints.
- Add allow/block action and persist reason.
- Update attendance row model to include `entryAllowed`.
- Guard PRESENT action for blocked students.

