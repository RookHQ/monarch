#!/bin/bash
# Captures the pictures of the /ios/ and /android/ pages of the docs site, in
# light and dark: the showcase screen of each sample app (the Overview of the
# demo workspace Acme Studio) on a phone and a tablet, the eight category
# tiles and the three small cards around the hero phone. Both platforms draw
# the same compositions with the same data (platforms/ios/Sample/Sources/Showcase,
# platforms/android/sample/.../showcase). The images are written to
# apps/docs/public/native/ as WebP, each at most 150 KB.
#
#   apps/docs/scripts/capture-native-screens.sh [ios|android|all]
#
# iOS: builds the catalogue app (platforms/ios/Scripts/verify.sh --sample),
# boots an iPhone and an iPad simulator, overrides the status bar (9:41, full
# battery and bars), switches the system appearance and launches the app with
# -MonarchShowcase YES. The tiles and cards are rendered by the preview host
# (platforms/ios/Scripts/build-previews.sh landing-tile-… landing-float-…).
# Simulators it booted are shut down again. Needs Xcode, XcodeGen and oxipng.
# Set MONARCH_SKIP_BUILD=1 to reuse an existing sample build.
#
# Android: runs LandingScreensTest in platforms/android/sample (Robolectric,
# no emulator): the showcase at 412x915dp (xxhdpi) and 800x1280dp (xhdpi),
# the tiles and cards at xxhdpi. The page draws the status bar over the space
# this script adds above each screen, filled with the app's own canvas
# colour. Needs a JDK 21 (JAVA_HOME) and the Android SDK.
#
# Processing needs python3 with Pillow (WebP support).
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
root="$(cd "$here/../../.." && pwd)"
out="$root/apps/docs/public/native"
which="${1:-all}"
raw="$(mktemp -d)"
trap 'rm -rf "$raw"' EXIT
mkdir -p "$out"

IPHONE="${MONARCH_IPHONE:-iPhone 17 Pro}"
IPAD="${MONARCH_IPAD:-iPad Pro 11-inch (M5)}"

capture_ios() {
    local ios="$root/platforms/ios"
    if [[ -z "${DEVELOPER_DIR:-}" && -d /Applications/Xcode.app ]]; then
        export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
    fi
    if [[ "${MONARCH_SKIP_BUILD:-}" != 1 ]]; then
        (cd "$ios" && Scripts/verify.sh --sample)
    fi
    local app="$ios/.build/xcode-sample/Build/Products/Debug-iphonesimulator/MonarchCatalogue.app"
    [[ -d "$app" ]] || { echo "capture: $app is missing; build the sample first" >&2; exit 1; }
    local bundle
    bundle="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app/Info.plist")"
    local pair name device udid booted nine_forty_one
    # 9:41 on a Tuesday in the host's time zone, which the simulator shows (ISO 8601 with milliseconds and +hh:mm, the only form simctl takes).
    nine_forty_one="$(date -j -f "%Y-%m-%d %H:%M:%S" "2026-01-06 09:41:00" +%Y-%m-%dT%H:%M:%S.000%z | sed -E 's/([0-9]{2})$/:\1/')"
    for pair in "iphone:$IPHONE" "ipad:$IPAD"; do
        name="${pair%%:*}"
        device="${pair#*:}"
        udid="$(xcrun simctl list devices available | grep -F "    $device (" | head -1 | grep -Eo '[0-9A-F-]{36}' || true)"
        [[ -n "$udid" ]] || { echo "capture: no simulator named $device" >&2; exit 1; }
        booted=0
        if ! xcrun simctl list devices booted | grep -q "$udid"; then
            xcrun simctl boot "$udid"
            booted=1
        fi
        xcrun simctl bootstatus "$udid" -b >/dev/null
        # A freshly booted simulator shows a few system notifications; let them pass.
        if [[ "$booted" == 1 ]]; then sleep 30; fi
        # The iPad status bar shows the date as well as the time.
        xcrun simctl status_bar "$udid" override --time "$nine_forty_one" --dataNetwork wifi --wifiMode active \
            --wifiBars 3 --cellularMode active --cellularBars 4 --batteryState discharging --batteryLevel 100
        xcrun simctl install "$udid" "$app"
        for theme in light dark; do
            xcrun simctl ui "$udid" appearance "$theme"
            xcrun simctl terminate "$udid" "$bundle" >/dev/null 2>&1 || true
            xcrun simctl launch "$udid" "$bundle" -MonarchShowcase YES >/dev/null
            # Let the launch animation and the first layout settle.
            sleep 5
            xcrun simctl io "$udid" screenshot --type=png "$raw/ios-$name.$theme.png" >/dev/null
        done
        xcrun simctl ui "$udid" appearance light
        xcrun simctl status_bar "$udid" clear
        xcrun simctl uninstall "$udid" "$bundle" || true
        if [[ "$booted" == 1 ]]; then xcrun simctl shutdown "$udid"; fi
    done
    local tiles=(actions forms navigation data-display feedback overlays theming intelligence)
    local floats=(toggle progress badges)
    local ids=() id
    for id in "${tiles[@]}"; do ids+=("landing-tile-$id"); done
    for id in "${floats[@]}"; do ids+=("landing-float-$id"); done
    (cd "$ios" && Scripts/build-previews.sh --out "$raw/host" "${ids[@]}")
    for file in "$raw"/host/landing-*.png; do
        id="$(basename "$file")"
        mv "$file" "$raw/ios-${id#landing-}"
    done
}

capture_android() {
    (cd "$root/platforms/android" &&
        MONARCH_LANDING_SHOTS="$raw" ./gradlew :sample:testDebugUnitTest \
            --tests 'com.rookhq.monarch.sample.LandingScreensTest' --rerun --console=plain)
}

case "$which" in
    ios) capture_ios ;;
    android) capture_android ;;
    all)
        capture_ios
        capture_android
        ;;
    *)
        echo "capture: unknown target $which (ios, android or all)" >&2
        exit 2
        ;;
esac

python3 "$here/native-screens.py" "$raw" "$out" "$root/apps/docs/src/features/native/native-screens.json"
