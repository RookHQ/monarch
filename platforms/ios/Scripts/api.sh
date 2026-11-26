#!/bin/bash
# Public API check. Dumps the public declarations of MonarchUI and
# MonarchTokens (from the compiler's symbol graph) and compares them with the
# committed baseline in api/. Experimental (@_spi(Experimental)) declarations
# are not part of api/MonarchUI.txt: they are in api/MonarchUI.experimental.txt,
# which lists them separately so a change to experimental API is still visible
# in a review. The script also fails when a stable declaration mentions an
# experimental one (Scripts/check-spi.py).
#
#   Scripts/api.sh            fail if the public API differs from the baseline
#   Scripts/api.sh --update   rewrite the baseline after an intended change
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

dump() {
    # Newer SwiftPM also tries the test bundle ("MonarchPackageTests") and exits 1 for it after it has written the
    # graphs of every library; the flatten step below fails loudly when a library's graph is missing.
    swift package dump-symbol-graph --minimum-access-level public --skip-synthesized-members "$@" >/dev/null || true
    graphs="$(swift build --show-bin-path)/../symbolgraph"
    [[ -d "$graphs" ]] || graphs="$(find .build -type d -name symbolgraph | head -n 1)"
}

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

dump
for module in MonarchTokens MonarchUI; do
    swift Scripts/flatten-symbol-graph.swift "$graphs" "$module" "$work/$module.txt"
done

# The same graphs with the SPI symbols: the extra lines are the experimental API.
dump --include-spi-symbols
swift Scripts/flatten-symbol-graph.swift "$graphs" MonarchUI "$work/MonarchUI.all.txt"
{ grep -vxFf "$work/MonarchUI.txt" "$work/MonarchUI.all.txt" || true; } >"$work/MonarchUI.experimental.txt"

status=0
python3 Scripts/check-spi.py "$graphs" || status=1

for module in MonarchTokens MonarchUI MonarchUI.experimental; do
    if [[ "${1:-}" == "--update" ]]; then
        mkdir -p api
        cp "$work/$module.txt" "api/$module.txt"
        echo "api: wrote api/$module.txt ($(wc -l <"api/$module.txt" | tr -d ' ') declarations)"
    elif diff -u "api/$module.txt" "$work/$module.txt"; then
        echo "api: $module matches the baseline ($(wc -l <"api/$module.txt" | tr -d ' ') declarations)"
    else
        echo "api: $module differs from api/$module.txt. If the change is intended, run Scripts/api.sh --update and record it in CHANGELOG.md." >&2
        status=1
    fi
done
exit $status
