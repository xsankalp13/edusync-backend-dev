#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

log() { printf "[HRMS Preflight] %s\n" "$*"; }
error() { printf "[HRMS Preflight][ERROR] %s\n" "$*" >&2; }

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  cat <<'USAGE'
Usage: hrms_migration_preflight.sh [--help|-h]

This script performs safety checks before running Flyway migrations for HRMS.
It validates presence of required migration files, required tools and environment variables.
USAGE
  exit 0
fi

log "Starting migration preflight checks..."

PROFILE="${SPRING_PROFILES_ACTIVE:-staging}"
BASELINE="${FLYWAY_BASELINE_ON_MIGRATE:-false}"
ATTENDANCE_POLICY="${HRMS_PAYROLL_ATTENDANCE_POLICY:-TREAT_UNMARKED_AS_ABSENT}"

if [[ ! -f "pom.xml" ]]; then
  error "pom.xml not found. Run from project root or keep script location unchanged."
  exit 2
fi

MIGRATION_EXPECTED="src/main/resources/db/migration/V20260404_01__hrms_initial_schema.sql"
if [[ ! -f "$MIGRATION_EXPECTED" ]]; then
  error "Required migration file is missing: $MIGRATION_EXPECTED"
  exit 3
fi

case "$ATTENDANCE_POLICY" in
  TREAT_UNMARKED_AS_ABSENT|MARKED_ONLY|FAIL_ON_PARTIAL) ;;
  *)
    echo "[ERROR] Invalid HRMS_PAYROLL_ATTENDANCE_POLICY='$ATTENDANCE_POLICY'"
    echo "        Allowed: TREAT_UNMARKED_AS_ABSENT, MARKED_ONLY, FAIL_ON_PARTIAL"
    exit 1
    ;;
esac

if [[ "$PROFILE" == "prod" || "$PROFILE" == "staging" ]]; then
  if [[ "$BASELINE" == "true" ]]; then
    echo "[WARN] FLYWAY_BASELINE_ON_MIGRATE=true on $PROFILE"
    echo "       Ensure this is an approved one-time baseline operation."
  fi
fi

if ! command -v mvn >/dev/null 2>&1; then
  error "Maven (mvn) not found in PATH"
  exit 4
fi

# Check for common DB CLIs used in operational scripts (best-effort)
if command -v psql >/dev/null 2>&1; then
  log "psql available"
else
  log "psql not found; some DB validation steps may be skipped"
fi
if command -v mysql >/dev/null 2>&1; then
  log "mysql client available"
fi

log "Profile: $PROFILE"
log "Flyway baseline-on-migrate: $BASELINE"
log "Attendance policy: $ATTENDANCE_POLICY"
log "Migration file check: OK"
log "Maven availability: OK"
log "SUCCESS"

