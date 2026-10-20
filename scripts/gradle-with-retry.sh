#!/usr/bin/env bash
#
# Run a Gradle task, retrying on transient network failures.
#
# Why this exists
# ---------------
# GitHub's hosted runners share egress IP addresses, and Maven Central
# occasionally answers a burst of requests with `403 Forbidden` or a 5xx. That
# failure is about the network, not about the code, and it has taken down
# otherwise-green builds - including one on 2026-10-20 where the very first
# Gradle step could not resolve the Android Gradle Plugin.
#
# This wrapper distinguishes a *network* failure from a *build* failure: it
# retries only when the log shows a dependency-resolution error, and it fails
# immediately on a real compilation or test failure. A broken test is never
# retried into a pass.
#
# Usage:
#   scripts/gradle-with-retry.sh :core:test
#   scripts/gradle-with-retry.sh :android:app:assembleDebug

set -uo pipefail

MAX_ATTEMPTS="${GRADLE_MAX_ATTEMPTS:-3}"
DELAY_SECONDS="${GRADLE_RETRY_DELAY:-20}"

if [ "$#" -eq 0 ]; then
    echo "usage: $0 <gradle task> [task...]" >&2
    exit 2
fi

# Patterns that indicate the failure was the network, not the build.
NETWORK_PATTERNS=(
    "Could not GET"
    "Could not get resource"
    "Could not resolve"
    "Received status code 40"
    "Received status code 50"
    "Connection reset"
    "Connection timed out"
    "Read timed out"
    "Premature end of Content-Length"
    "peer not authenticated"
    "Temporary failure in name resolution"
    "503 Service Unavailable"
    "502 Bad Gateway"
    "504 Gateway"
)

is_network_failure() {
    local log_file="$1"
    local pattern
    for pattern in "${NETWORK_PATTERNS[@]}"; do
        if grep -qF "$pattern" "$log_file"; then
            return 0
        fi
    done
    return 1
}

attempt=1
while true; do
    log_file="$(mktemp)"
    echo "::group::Gradle attempt $attempt of $MAX_ATTEMPTS: $*"

    ./gradlew --no-daemon "$@" 2>&1 | tee "$log_file"
    status="${PIPESTATUS[0]}"

    echo "::endgroup::"

    if [ "$status" -eq 0 ]; then
        rm -f "$log_file"
        exit 0
    fi

    if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
        echo "::error::Gradle failed after $MAX_ATTEMPTS attempts. Last failure was not classified as a network error."
        rm -f "$log_file"
        exit "$status"
    fi

    if ! is_network_failure "$log_file"; then
        echo "::error::Gradle failed with a non-network error; not retrying."
        rm -f "$log_file"
        exit "$status"
    fi

    echo "::warning::Gradle failed on attempt $attempt with a dependency-resolution error (likely a transient Maven Central 403/5xx from a shared runner IP). Retrying in ${DELAY_SECONDS}s."
    rm -f "$log_file"
    sleep "$DELAY_SECONDS"
    attempt=$((attempt + 1))
done
