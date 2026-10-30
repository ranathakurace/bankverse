#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
suite="${1:-all}"
browser="${2:-chromium}"
case "$suite" in all|api|ui|smoke|edge) ;; *) echo "Unknown suite" >&2; exit 2 ;; esac
case "$browser" in chromium|firefox|webkit) ;; *) echo "Unknown browser" >&2; exit 2 ;; esac
export PLAYWRIGHT_BROWSERS_PATH="${PLAYWRIGHT_BROWSERS_PATH:-$PWD/.browsers}"
if [[ "${3:-}" == "install" ]]; then
  mvn --batch-mode test-compile exec:java -Dexec.mainClass=com.microsoft.playwright.CLI "-Dexec.args=install $browser"
fi
args=(--batch-mode test "-Dbrowser=$browser" "-Dheadless=${HEADLESS:-true}")
if [[ "$suite" != "all" ]]; then args+=("-P$suite"); fi
mvn "${args[@]}"
