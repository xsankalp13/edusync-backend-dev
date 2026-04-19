#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
HOOKS_DIR="$ROOT_DIR/.git/hooks"
SOURCE_HOOK="$ROOT_DIR/scripts/git-hooks/pre-commit"

if [[ ! -d "$HOOKS_DIR" ]]; then
  echo "[setup-git-hooks] .git/hooks not found. Are you in a git repo?"
  exit 1
fi

if [[ ! -f "$SOURCE_HOOK" ]]; then
  echo "[setup-git-hooks] Source hook not found: $SOURCE_HOOK"
  exit 2
fi

cp "$SOURCE_HOOK" "$HOOKS_DIR/pre-commit"
chmod +x "$HOOKS_DIR/pre-commit"

echo "[setup-git-hooks] Installed pre-commit hook"

