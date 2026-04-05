# HRMS Regression Baseline Report

Date: 2026-04-04

## Command Used

```bash
./scripts/hrms_regression_suite.sh
```

## Suite Composition

- `LeaveManagementLifecycleFlowTest`
- `StaffSalaryMappingServiceImplTest`
- `PayrollServiceImplTest`
- `PayrollLifecycleFlowTest`
- `PayrollLeaveEndToEndFlowTest`

## Result Summary

- Total tests: 24
- Failures: 0
- Errors: 0
- Skipped: 0
- Build: SUCCESS

## Notes

- Coverage targets critical payroll/leave flow behavior and attendance/statutory hardening.
- Some JVM warnings from Mockito dynamic agent loading are non-failing and informational.

