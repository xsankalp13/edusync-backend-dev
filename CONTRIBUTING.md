Contributing to edusync-backend-dev

Thank you for your interest in contributing! This repository contains the backend services for the Edusync platform. We welcome contributions that improve reliability, observability, documentation, and developer experience.

Getting started

1. Clone the repository:
   git clone git@github.com:your-org/edusync-backend-dev.git
2. Prerequisites:
   - JDK 17+
   - Maven 3.8+
   - Docker (optional, for running dependent services)
3. Run locally:
   - Start dependent services: docker-compose up
   - Run the app: ./mvnw spring-boot:run
4. Run tests: ./mvnw test

Branching and commits

- Use feature/ or fix/ or chore/ prefixes: e.g. feature/hrms-bulk-import, fix/staff-bank-null
- Keep commits small and focused; use present-tense brief summary (<= 50 chars) and a detailed body if needed.
- Example commit message:

  feat: add staff bank import CSV template

  - add exportCsvTemplate in StaffBankDetailsService
  - add sample CSV in docs/

Pull request checklist

- [ ] Run unit and integration tests locally
- [ ] Add or update documentation where necessary
- [ ] Update CHANGELOG.md under Unreleased
- [ ] Ensure code follows existing style and formatting rules

Thank you for contributing!
