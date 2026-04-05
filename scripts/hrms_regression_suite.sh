#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

TEST_SET="PayrollServiceImplTest,PayrollLeaveEndToEndFlowTest,StaffSalaryMappingServiceImplTest,LeaveManagementLifecycleFlowTest,PayrollLifecycleFlowTest"

echo "[HRMS Regression] Running critical HRMS suite..."
echo "[HRMS Regression] Tests: $TEST_SET"

mvn -Dtest="$TEST_SET" test

echo "[HRMS Regression] SUCCESS"

