#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[HRMS Preflight] Starting migration preflight checks..."

PROFILE="${SPRING_PROFILES_ACTIVE:-staging}"
BASELINE="${FLYWAY_BASELINE_ON_MIGRATE:-false}"
ATTENDANCE_POLICY="${HRMS_PAYROLL_ATTENDANCE_POLICY:-TREAT_UNMARKED_AS_ABSENT}"

if [[ ! -f "pom.xml" ]]; then
  echo "[ERROR] pom.xml not found. Run from project root or keep script location unchanged."
  exit 1
fi

if [[ ! -f "src/main/resources/db/migration/V20260404_01__hrms_initial_schema.sql" ]]; then
  echo "[ERROR] Required migration file is missing: src/main/resources/db/migration/V20260404_01__hrms_initial_schema.sql"
  exit 1
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
  echo "[ERROR] Maven (mvn) not found in PATH"
  exit 1
fi

echo "[HRMS Preflight] Profile: $PROFILE"
echo "[HRMS Preflight] Flyway baseline-on-migrate: $BASELINE"
echo "[HRMS Preflight] Attendance policy: $ATTENDANCE_POLICY"
echo "[HRMS Preflight] Migration file check: OK"
echo "[HRMS Preflight] Maven availability: OK"
echo "[HRMS Preflight] SUCCESS"

