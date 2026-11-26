#!/bin/bash
# Lints the hand-written Swift with swift-format, which ships with the Swift
# toolchain. Generated files (Sources/MonarchTokens and every Generated
# folder) are not linted.
#
#   Scripts/lint.sh                    lint everything
#   Scripts/lint.sh --fix              reformat in place, then lint
#   Scripts/lint.sh [--fix] PATH...    only these files or folders
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

fix=false
if [[ "${1:-}" == "--fix" ]]; then
    fix=true
    shift
fi

roots=("$@")
if [[ ${#roots[@]} -eq 0 ]]; then
    roots=(Package.swift Sources/MonarchUI Tests Scripts Sample/Sources)
fi

files=()
while IFS= read -r file; do files+=("$file"); done < <(
    find "${roots[@]}" -name '*.swift' -not -path '*/Generated/*' | sort
)
if [[ ${#files[@]} -eq 0 ]]; then
    echo "lint: no Swift files in: ${roots[*]}" >&2
    exit 1
fi

if $fix; then
    swift format format --in-place --configuration .swift-format "${files[@]}"
fi
swift format lint --strict --configuration .swift-format "${files[@]}"
echo "lint: ok (${#files[@]} files)"
python3 "$(dirname "$0")/check-clock.py"
