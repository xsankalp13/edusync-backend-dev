# Flyway Rollout Checklist (HRMS)

## Purpose

Use this checklist to safely roll out HRMS Flyway migrations across environments where schema may already exist.

Primary runbook: `docs/hrms/HRMS_MIGRATION_RUNBOOK.md`
Automation scripts:
- `scripts/hrms_migration_preflight.sh`
- `scripts/hrms_regression_suite.sh`

## Current Wiring

- Dependency: `org.flywaydb:flyway-core` in `pom.xml`
- Flyway enabled in `src/main/resources/application.yml`
- Locations: `classpath:db/migration`
- Baseline on migrate: `true`
- Hibernate mode: `ddl-auto: validate` (dev/prod)

## Migration Asset

- `src/main/resources/db/migration/V20260404_01__hrms_initial_schema.sql`

## Environment Verification Matrix

| Environment | DB Has Existing HRMS Tables | Expected Behavior | Owner Check |
|---|---|---|---|
| Dev | Yes/No | Flyway creates baseline history and validates/executes migrations | Backend developer |
| Staging | Usually Yes | Baseline safely, then apply pending versions | QA + Backend |
| Prod | Yes | Baseline and migrate with rollback plan approved | DevOps + Backend lead |

## Pre-Deploy Checklist

- [ ] Backup database or verify snapshot policy.
- [ ] Confirm `flyway_schema_history` table policy is approved.
- [ ] Confirm migration scripts are idempotent where needed.
- [ ] Confirm no `ddl-auto:update` remains in active profiles.
- [ ] Dry run against staging-like database clone.

## Suggested Validation Commands

```bash
cd "/Users/ashish/Professional/Projects/Shiksha Intelligence/edusync-backend-dev"
mvn -Dtest=PayrollLeaveEndToEndFlowTest,PayrollServiceImplTest,StaffSalaryMappingServiceImplTest test
```

```bash
cd "/Users/ashish/Professional/Projects/Shiksha Intelligence/edusync-backend-dev"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Post-Deploy Checks

- [ ] Application boots with no Flyway validation failures.
- [ ] `flyway_schema_history` contains baseline/version entries.
- [ ] HRMS calendar, leave, payroll, and payslip APIs respond as expected.
- [ ] Payroll run creation succeeds with attendance + LOP logic.

## Regression Baseline Artifact

- Latest baseline report: `docs/hrms/artifacts/2026-04-04_hrms_regression_baseline.md`

## Open Decisions

- Keep `baseline-on-migrate: true` globally, or move to environment-specific override.
- Confirm production rollback procedure for migration failures.



