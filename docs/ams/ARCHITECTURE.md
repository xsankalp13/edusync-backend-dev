AMS Architecture Overview

Overview

The AMS (Attendance Management System) module handles student and staff attendance capture, processing and reporting. It is implemented as a logical module inside the monolithic backend and exposes REST endpoints under `/api/ams/*`.

Key components

- Controllers: REST endpoints for attendance (punch, bulk imports, reports)
- Services: Business logic for validation, geo-fencing and attendance rules
- Repositories: Spring Data JPA repositories for persistence

Integration

- Pushes attendance events into the central event bus for downstream processing
- Reads user and enrollment data from `uis` module tables

Deployment notes

- Designed for horizontal scaling on the service that receives events (worker pool or async processing). Keep DB access limited and use pagination for large exports.
