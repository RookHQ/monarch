#!/bin/bash
# Renders documentation previews: ONE image per component of its primary inventory example (the first, the
# one the docs page shows first), drawn by the real component in the preview host app (PreviewHost/) on the
# iPhone 17 Pro simulator, in the theme's canvas colour, cropped to the content plus 24 points of padding.
# No labels, no stacking, no status bar. The screenshot is taken at 3x and published at 2x. A curated showcase
# (PreviewHost/Sources/Showcases.swift) is used for a primitive that has one, the first example otherwise.
# The text is set in Poppins (MonarchFontsPoppins, D10.4); the regression baselines keep the system font.
# Locale, calendar and time zone are pinned (British English, 24-hour, Monday first, UTC). A screen taller than 640 points is cut at
# the last gap between blocks (process-previews.py).
#
#   Scripts/build-previews.sh [--out DIR] [--themes light,dark,...] ID [ID ...]
#
# --out     where the images go (default platforms/ios/previews). A pilot goes outside the repository.
# --themes  light, dark, hc-light, hc-dark (default light,dark)
# Image names: <id>.<theme>.png. These are not the regression baselines (Tests/Snapshots, Scripts/verify.sh
# --snapshots), which stack every fixture of a component.
#
# Every image stays at or under 150 KB (Scripts/limit-previews.py: palette, then scale).
# Needs Xcode, xcodegen, oxipng and python3 with Pillow. Boots the simulator and shuts it down again if it
# booted it; the status bar is fixed to 9:41 while it runs. Run it through the heavy lock on the shared Mac.
set -euo pipefail
cd "$(dirname "$0")/.."

out="previews"
themes="light,dark"
ids=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --out) out="$2"; shift 2 ;;
        --themes) themes="$2"; shift 2 ;;
        -*) echo "build-previews: unknown option $1" >&2; exit 2 ;;
        *) ids+=("$1"); shift ;;
    esac
done
[[ ${#ids[@]} -gt 0 ]] || { echo "build-previews: give at least one inventory id" >&2; exit 2; }
export DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"

device="iPhone 17 Pro"
# The same device name can exist on several runtimes; simctl then refuses a name. Use the id of the one on the
# newest runtime, which is the one xcodebuild's OS=latest destination builds for.
udid="$(xcrun simctl list devices available -j | python3 -c '
import json, sys
data = json.load(sys.stdin)["devices"]
best = None
for runtime, devices in data.items():
    if "iOS" not in runtime:
        continue
    version = tuple(int(part) for part in runtime.rsplit("iOS-", 1)[-1].split("-") if part.isdigit())
    for item in devices:
        if item["name"] == sys.argv[1] and (best is None or version > best[0]):
            best = (version, item["udid"])
print(best[1] if best else sys.argv[1])
' "$device")"
bundle="dev.monarchui.previewhost"
derived=".build/preview-host"
raw="$(mktemp -d)"
mkdir -p "$out" .build

Scripts/generate.py >/dev/null
(cd PreviewHost && xcodegen generate --quiet --spec project.yml)
xcodebuild build -project PreviewHost/MonarchPreviewHost.xcodeproj -scheme MonarchPreviewHost \
    -destination "platform=iOS Simulator,name=$device,OS=latest" -derivedDataPath "$derived" \
    CODE_SIGNING_ALLOWED=NO >.build/preview-host-build.log 2>&1 || {
    grep -E "error:" .build/preview-host-build.log | awk '!seen[$0]++' | head -n 40 >&2
    echo "build-previews: the build failed. Log: $PWD/.build/preview-host-build.log" >&2
    exit 1
}

booted_before="$(xcrun simctl list devices booted | grep -c "$udid" || true)"
xcrun simctl boot "$udid" 2>/dev/null || true
xcrun simctl bootstatus "$udid" -b >/dev/null
cleanup() {
    xcrun simctl status_bar "$udid" clear 2>/dev/null || true
    if [[ "$booted_before" == 0 ]]; then xcrun simctl shutdown "$udid" 2>/dev/null || true; fi
    rm -rf "$raw"
}
trap cleanup EXIT
xcrun simctl status_bar "$udid" override --time "9:41" --batteryState charged --batteryLevel 100 --wifiBars 3 --cellularBars 4
xcrun simctl install "$udid" "$derived/Build/Products/Debug-iphonesimulator/MonarchPreviewHost.app"
data="$(xcrun simctl get_app_container "$udid" "$bundle" data)/Documents"
mkdir -p "$data"

# The screen width in points, read from the simulator's own first screenshot, divided by its scale: an
# iPhone 17 Pro is 402 points wide.
screen_points=402

failed=()
for theme in ${themes//,/ }; do
    for id in "${ids[@]}"; do
        rm -f "$data/preview-rect.txt"
        xcrun simctl terminate "$udid" "$bundle" >/dev/null 2>&1 || true
        # British English, 24-hour clock and UTC on every machine; the app sets its own time zone as well.
        SIMCTL_CHILD_TZ=UTC xcrun simctl launch "$udid" "$bundle" \
            -AppleLocale en_GB -AppleLanguages "(en-GB)" -AppleICUForce24HourTime YES \
            -preview "$id" -theme "$theme" >/dev/null
        tries=0
        until grep -q "^$id " "$data/preview-rect.txt" 2>/dev/null || [[ $tries -ge 60 ]]; do
            sleep 0.25
            tries=$((tries + 1))
        done
        if ! grep -q "^$id " "$data/preview-rect.txt" 2>/dev/null; then
            failed+=("$id.$theme")
            continue
        fi
        sleep 0.3
        xcrun simctl io "$udid" screenshot "$raw/full.png" >/dev/null 2>&1
        python3 Scripts/process-previews.py "$raw/full.png" "$data/preview-rect.txt" "$screen_points" "$out/$id.$theme.png"
        # An example taller than one screen strip (the rect file says so: capped, not presented, with the natural
        # height) is shown again from each next offset and the strips are stitched.
        read -r _ _ _ _ _ capped_flag presented_flag natural_height <<<"$(cat "$data/preview-rect.txt")"
        if [[ "${capped_flag:-0}" == 1 && "${presented_flag:-0}" == 0 && -n "${natural_height:-}" ]]; then
            strips=("$out/$id.$theme.png")
            offset=740
            while python3 -c "import sys; sys.exit(0 if float('$natural_height') > $offset and $offset < 2900 else 1)"; do
                rm -f "$data/preview-rect.txt"
                xcrun simctl terminate "$udid" "$bundle" >/dev/null 2>&1 || true
                SIMCTL_CHILD_TZ=UTC xcrun simctl launch "$udid" "$bundle" \
                    -AppleLocale en_GB -AppleLanguages "(en-GB)" -AppleICUForce24HourTime YES \
                    -preview "$id" -theme "$theme" -offset "$offset" >/dev/null
                tries=0
                until grep -q "^$id " "$data/preview-rect.txt" 2>/dev/null || [[ $tries -ge 60 ]]; do
                    sleep 0.25
                    tries=$((tries + 1))
                done
                grep -q "^$id " "$data/preview-rect.txt" 2>/dev/null || break
                sleep 0.3
                xcrun simctl io "$udid" screenshot "$raw/full.png" >/dev/null 2>&1
                python3 Scripts/process-previews.py "$raw/full.png" "$data/preview-rect.txt" "$screen_points" "$raw/strip-$offset.png"
                strips+=("$raw/strip-$offset.png")
                offset=$((offset + 740))
            done
            python3 Scripts/stitch-previews.py "$out/$id.$theme.png" "${strips[@]}"
        fi
        echo "$id.$theme"
    done
done
xcrun simctl terminate "$udid" "$bundle" >/dev/null 2>&1 || true
# Only the images rendered in this run are recompressed; the rest of the folder is already compressed.
rendered=()
for id in "${ids[@]}"; do
    for theme in ${themes//,/ }; do rendered+=("$out/$id.$theme.png"); done
done
oxipng -o max --strip safe -q "${rendered[@]}"
python3 Scripts/limit-previews.py "$out" 150000
if [[ ${#failed[@]} -gt 0 ]]; then
    echo "build-previews: no layout reported for: ${failed[*]}" >&2
    exit 1
fi
echo "build-previews: $(ls "$out" | wc -l | tr -d ' ') images in $out"
