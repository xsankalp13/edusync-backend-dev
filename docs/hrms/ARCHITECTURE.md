HRMS Architecture Overview

Overview

The HRMS module manages staff lifecycle: onboarding, payroll, leave, approvals and documents. It is implemented as a logical module inside the monolith and exposes endpoints under `/api/hrms/*`.

Key components

- Controllers: HTTP API for HR operations
- Services: Business logic for payroll, statutory calculations and approvals
- Repositories: Spring Data JPA repositories and Flyway migrations

Operational notes

- Migration runbooks are stored in `docs/hrms/` and scripts are under `scripts/`.
- Use preflight checks (`scripts/hrms_migration_preflight.sh`) before applying Flyway changes in production.

Security and data handling

- Sensitive staff information (bank account, IDs) is stored encrypted/filtered and access-controlled; follow existing patterns in `StaffSensitiveInfo` entity and related services.
