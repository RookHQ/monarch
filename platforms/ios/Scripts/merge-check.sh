#!/bin/bash
# Run in the main tree after copying engineers' new files in.
#
#   1. Regenerates everything generated (components.json, compiled examples,
#      fixture registry, sample registry) and validates the inventory.
#   2. Rewrites the public API baseline in api/ and shows what changed.
#   3. Reports how the inventory compares with ../parity.json.
#   4. Runs the host gates: Scripts/verify.sh --host-only, which ends with a
#      compile of the package and its tests for the iOS Simulator
#      (build-for-testing; no simulator is booted). Test code under
#      #if os(iOS) is only compiled there, so a simulator-only test that does
#      not compile fails here instead of in the full run.
#
# The simulator tests, documentation and sample builds are in the full
# Scripts/verify.sh; run it after this passes.
set -euo pipefail
cd "$(dirname "$0")/.."

# Selects the Xcode for this process only (no system setting is changed) and
# prints it. DEVELOPER_DIR is honoured if set; otherwise `xcode-select -p` when
# it is an Xcode, else /Applications/Xcode.app, else the first
# /Applications/Xcode*.app. The Command Line Tools are never used: they have no
# simulator tools (xcrun simctl) and no Swift Testing module.
select_xcode() {
    local dir="${DEVELOPER_DIR:-}" candidate
    if [[ -z "$dir" ]]; then
        dir="$(xcode-select -p 2>/dev/null || true)"
        if [[ "$dir" != *.app/Contents/Developer ]]; then
            dir=""
            for candidate in /Applications/Xcode.app/Contents/Developer /Applications/Xcode*.app/Contents/Developer; do
                if [[ -x "$candidate/usr/bin/xcodebuild" ]]; then
                    dir="$candidate"
                    break
                fi
            done
        fi
    fi
    if [[ -z "$dir" || "$dir" != *.app/Contents/Developer || ! -x "$dir/usr/bin/xcodebuild" ]]; then
        echo "$(basename "$0"): no Xcode found (DEVELOPER_DIR=${DEVELOPER_DIR:-unset}, xcode-select -p=$(xcode-select -p 2>/dev/null || echo none))." >&2
        echo "Install Xcode in /Applications or set DEVELOPER_DIR to <Xcode>.app/Contents/Developer." >&2
        exit 1
    fi
    if [[ "${DEVELOPER_DIR:-}" != "$dir" ]]; then
        echo "$(basename "$0"): using Xcode at $dir" >&2
    fi
    export DEVELOPER_DIR="$dir"
}
select_xcode

printf '\n==> Regenerate generated files\n'
Scripts/generate.py

printf '\n==> Update the public API baseline\n'
before="$(mktemp -d)"
trap 'rm -rf "$before"' EXIT
cp api/*.txt "$before"/
Scripts/api.sh --update
for file in api/*.txt; do
    added="$(diff "$before/$(basename "$file")" "$file" | grep -c '^>' || true)"
    removed="$(diff "$before/$(basename "$file")" "$file" | grep -c '^<' || true)"
    echo "api: $(basename "$file"): $added added, $removed removed"
    # Removed or changed declarations are breaking changes; show them.
    diff "$before/$(basename "$file")" "$file" | grep '^<' || true
done

printf '\n==> Parity with ../parity.json (report only)\n'
Scripts/generate.py --parity

Scripts/verify.sh --host-only

printf '\nmerge-check: passed. Now run Scripts/verify.sh for the simulator tests, documentation and sample gates.\n'
