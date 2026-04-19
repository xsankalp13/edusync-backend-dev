#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

log() { printf "[ci-build] %s\n" "$*"; }

log "Running CI build"
MAVEN_OPTS=${MAVEN_OPTS:-"-Xmx1024m"}
export MAVEN_OPTS

./mvnw -B -DskipTests=false clean verify

log "CI build finished"
