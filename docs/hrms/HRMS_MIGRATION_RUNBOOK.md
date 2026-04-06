# HRMS Migration Runbook

## Scope

This runbook operationalizes Flyway rollout for HRMS across dev, staging, and production.

## Preconditions

- Flyway dependency present in `pom.xml`.
- Migration scripts available in `src/main/resources/db/migration`.
- Active migration baseline script: `V20260404_01__hrms_initial_schema.sql`.
- `application.yml` configured with environment-safe baseline behavior.

## Finalized Rollout Policy

- `dev`: baseline can be enabled by default for local bootstrap.
- `staging`: baseline is explicit (`FLYWAY_BASELINE_ON_MIGRATE=false` by default).
- `prod`: baseline is explicit (`FLYWAY_BASELINE_ON_MIGRATE=false` by default).

## Attendance Policy Lock (Payroll)

- Finalized default: `TREAT_UNMARKED_AS_ABSENT`.
- Config key: `app.hrms.payroll.attendance.partial-mark-policy`.
- Supported values:
  - `TREAT_UNMARKED_AS_ABSENT`
  - `MARKED_ONLY`
  - `FAIL_ON_PARTIAL`

## Deployment Flow

1. Confirm environment variables:
   - `SPRING_PROFILES_ACTIVE`
   - `FLYWAY_BASELINE_ON_MIGRATE`
   - `HRMS_PAYROLL_ATTENDANCE_POLICY`
2. Take DB backup/snapshot.
3. Run migration preflight:
   - `./scripts/hrms_migration_preflight.sh`
4. Run regression subset:
   - `./scripts/hrms_regression_suite.sh`
5. Start application and monitor Flyway logs.
6. Verify `flyway_schema_history` entries.
7. Execute smoke checks for HRMS modules.

## Recommended Commands

```bash
cd "/Users/ashish/Professional/Projects/Shiksha Intelligence/edusync-backend-dev"
./scripts/hrms_migration_preflight.sh
```

```bash
cd "/Users/ashish/Professional/Projects/Shiksha Intelligence/edusync-backend-dev"
./scripts/hrms_regression_suite.sh
```

```bash
cd "/Users/ashish/Professional/Projects/Shiksha Intelligence/edusync-backend-dev"
SPRING_PROFILES_ACTIVE=staging FLYWAY_BASELINE_ON_MIGRATE=false mvn spring-boot:run
```

```bash
cd "/Users/ashish/Professional/Projects/Shiksha Intelligence/edusync-backend-dev"
SPRING_PROFILES_ACTIVE=prod FLYWAY_BASELINE_ON_MIGRATE=false mvn spring-boot:run
```

## Verification Checklist

- [ ] Flyway starts with expected baseline mode for target environment.
- [ ] No validation errors in migration logs.
- [ ] `flyway_schema_history` table exists and has expected versions.
- [ ] Payroll create/approve/disburse works after migration.
- [ ] Payslip read/pdf endpoints work after migration.

## Rollback Guidance

- Stop deployment if Flyway validation fails.
- Restore DB snapshot.
- Fix migration script in a new forward migration (do not edit applied versions).



