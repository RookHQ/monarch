#!/bin/bash
# Quality gates for Monarch on iOS. Nothing is changed outside platforms/ios,
# and no system setting is touched.
#
#   Scripts/verify.sh                     every gate (needs an iOS Simulator runtime and XcodeGen)
#   Scripts/verify.sh --host-only         the gates that need no simulator, including the API baseline,
#                                         plus the simulator compile of the package and its tests
#                                         (no simulator is booted)
#   Scripts/verify.sh --local             for a working copy: regenerate, lint, host build, all host
#                                         tests. Skips the merge-time gates (API baseline, stale check)
#                                         and the simulator.
#   Scripts/verify.sh --component NAME    the fast loop for one component: regenerate, lint its files,
#                                         host build, its own tests, its fixtures in the matrix and its
#                                         compiled examples, then compiles the package and its tests
#                                         for the iOS Simulator (code under #if os(iOS) is only built
#                                         there). NAME is a Swift type, a title or an inventory id.
#                                         No simulator is booted.
#   Scripts/verify.sh --snapshots [IDS]   the screenshot tests (Tests/MonarchUISnapshotTests) on the
#                                         simulator only. Boots MONARCH_IOS_DESTINATION; shuts down
#                                         what it booted. IDS is a comma-separated list of inventory
#                                         ids (for example badge,button); without it every component
#                                         runs. With MONARCH_SNAPSHOT_RECORD=1 the tests record the
#                                         baselines in Tests/Snapshots instead of comparing. Failed
#                                         compares write the actual and diff images to
#                                         .build/snapshot-failures and print their paths. Never
#                                         record on the host: its fonts differ.
#   Scripts/verify.sh --sample            regenerate, then build the catalogue app for the simulator
#                                         (no simulator is booted). The only mode besides the full
#                                         one that compiles Sample/.
#
# Environment:
#   DEVELOPER_DIR             the Xcode to use. Honoured if set (it must be an
#                             Xcode, not the Command Line Tools). Otherwise
#                             `xcode-select -p` when it is an Xcode, else
#                             /Applications/Xcode.app, else the first
#                             /Applications/Xcode*.app; for this process only.
#   MONARCH_IOS_DESTINATION   the xcodebuild destination for tests.
#                             Default: platform=iOS Simulator,name=iPhone 17 Pro,OS=latest
#   MONARCH_TEST_IDLE_SECONDS a test run that prints nothing for this long is
#                             stopped and the tests in flight are listed.
#                             Default: 600.
#   MONARCH_TEST_ALLOWANCE_SECONDS / MONARCH_TEST_MAX_SECONDS
#                             xcodebuild's per-test time allowance, a backstop
#                             behind the idle check. Defaults: 1800 and 3600.
set -euo pipefail
cd "$(dirname "$0")/.."

mode=full
component=""
snapshot_ids=""
case "${1:-}" in
    "") ;;
    --host-only) mode=host ;;
    --local) mode=local ;;
    --sample) mode=sample ;;
    --snapshots)
        mode=snapshots
        snapshot_ids="${2:-}"
        ;;
    --component)
        mode=component
        component="${2:?verify: --component needs a name}"
        ;;
    *)
        echo "verify: unknown option $1" >&2
        exit 2
        ;;
esac

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
destination="${MONARCH_IOS_DESTINATION:-platform=iOS Simulator,name=iPhone 17 Pro,OS=latest}"
derived=".build/xcode"
# The same flags for every swift invocation, so nothing is built twice.
swift_flags=(-Xswiftc -warnings-as-errors)
results='error:|✘|recorded an issue|Test run with'

step() { printf '\n==> %s\n' "$1"; }

# Shows why a build failed without hiding anything: every distinct error line,
# up to 200 lines from the first error on (notes, the code excerpt, the
# rest of the diagnostic), and any compiler crash report ("Stack dump",
# "Please submit a bug report"). The full log stays on disk.
#
# Checked by hand after changing it: a scratch copy of the package with a
# type error in MonarchButton.swift, then `Scripts/verify.sh --component
# MonarchButton`, prints the error with its file:line, the notes and the log
# path, and exits non-zero.
print_build_failure() {
    local log="$1"
    local first
    echo "---- errors ($(grep -c 'error:' "$log" || true) lines with error:) ----" >&2
    grep -E 'error:' "$log" | awk '!seen[$0]++' >&2 || true
    first="$(grep -n -m 1 -E 'error:' "$log" | cut -d: -f1 || true)"
    if [[ -n "$first" ]]; then
        echo "---- from the first error (up to 200 lines) ----" >&2
        sed -n "${first},$((first + 199))p" "$log" >&2
    fi
    if grep -q -E 'Stack dump|Please submit a bug report' "$log"; then
        echo "---- compiler crash ----" >&2
        grep -n -E -A 60 'Please submit a bug report|Stack dump' "$log" | head -n 200 >&2
    fi
    if [[ -z "$first" ]] && ! grep -q -E 'Stack dump|Please submit a bug report' "$log"; then
        echo "---- last 80 lines ----" >&2
        tail -n 80 "$log" >&2
    fi
    echo "verify: ${2:-the build} failed. Full log: $PWD/$log" >&2
}

# swift build --build-tests with the full output kept in .build/host-build.log.
# On success only the summary is shown; on failure print_build_failure.
host_build() {
    local log=.build/host-build.log
    mkdir -p .build
    if ! swift build --build-tests "${swift_flags[@]}" >"$log" 2>&1; then
        print_build_failure "$log"
        return 1
    fi
    grep -E 'warning:|Build complete' "$log" | tail -n 20 || true
}

# Lists the tests that started in a log and never finished: Swift Testing
# ("◇ Test x() started." / "✔ Test x() passed ...") and XCTest ("Test Case
# '-[A b]' started." / "... passed (0.1 seconds)."). Names are counted, so two
# tests with the same name in different suites are both tracked.
tests_in_flight() {
    LC_ALL=C sed -E 's/^[^A-Za-z]*//' "$1" | awk '
        # Swift Testing announces each argument of a parameterized test ("Test case
        # passing ...") and the run itself, but prints no end line for them.
        /^Test (case|run) / { next }
        /^(Test|Suite|Test Case) .* started\.?$/ {
            name = $0; sub(/ started\.?$/, "", name); open[name]++; next
        }
        /^(Test|Suite|Test Case) .* (passed|failed|skipped|was skipped)/ {
            name = $0; sub(/ (passed|failed|skipped|was skipped).*$/, "", name)
            if (open[name] > 0) open[name]--
        }
        END { for (name in open) if (open[name] > 0) print "  " name }
    ' | sort
}

# Stops a process and everything it started (swift test runs the tests in a
# helper process; xcodebuild in xctest/simulator processes).
kill_tree() {
    local pid="$1" child
    for child in $(pgrep -P "$pid" 2>/dev/null || true); do kill_tree "$child"; done
    kill -TERM "$pid" 2>/dev/null || true
}

# Runs a test command with its output in a log, and stops it when the log has
# not grown for MONARCH_TEST_IDLE_SECONDS: a hung test (for example a SwiftUI
# layout that never settles) otherwise keeps xctest busy with no output and no
# end. On a stop it prints the tests in flight and returns 124.
run_watched() {
    local log="$1"
    shift
    local idle="${MONARCH_TEST_IDLE_SECONDS:-600}" size last=-1 quiet=0 status=0
    mkdir -p .build
    "$@" >"$log" 2>&1 &
    local pid=$!
    while kill -0 "$pid" 2>/dev/null; do
        sleep 5
        size="$(wc -c <"$log")"
        if [[ "$size" != "$last" ]]; then
            last="$size"
            quiet=0
        else
            quiet=$((quiet + 5))
        fi
        if ((quiet >= idle)); then
            echo "verify: no test output for ${idle} s; stopping the run. Tests in flight:" >&2
            tests_in_flight "$log" >&2
            kill_tree "$pid"
            wait "$pid" 2>/dev/null || true
            echo "verify: full log: $PWD/$log" >&2
            return 124
        fi
    done
    wait "$pid" || status=$?
    return "$status"
}

# xcodebuild relaunches the test runner after a crash or a test timeout
# ("Restarting after unexpected exit, crash, or test timeout") and then reports
# every test that never ran as failed, which can bury one hang under hundreds
# of failures. This prints, first, for each restart: the last test that
# started before it, the tests still in flight and the 30 log lines before it.
report_restarts() {
    local log="$1" line last
    grep -n "Restarting after unexpected exit, crash, or test timeout" "$log" | cut -d: -f1 | while read -r line; do
        last="$(head -n "$line" "$log" | LC_ALL=C sed -E 's/^[^A-Za-z]*//' |
            grep -E '^(Test|Test Case) .* started' | grep -vE '^Test (case|run) ' | tail -n 1 || true)"
        echo "======== the test runner restarted (log line $line): the real cause is here ========" >&2
        echo "Last test started before the restart: ${last:-unknown}" >&2
        echo "Tests started and not finished before the restart:" >&2
        tests_in_flight <(head -n "$line" "$log") >&2
        echo "---- the 30 lines before the restart ----" >&2
        head -n $((line - 1)) "$log" | tail -n 30 >&2
        echo "======== tests reported as failing after this restart may never have run ========" >&2
    done || true # pipefail: grep finds no restart in an ordinary failure, which must not end the script before the errors are printed
}

# Prints the xcodebuild lines that matter and keeps the full log for failures.
# A test action runs under run_watched, with per-test XCTest allowances.
run_xcodebuild() {
    local log="$1"
    shift
    mkdir -p .build
    local runner=() status=0
    case " $* " in
        *" test "* | *" test-without-building "*)
            runner=(run_watched "$log")
            # A backstop only. The parameterized matrix tests (every fixture in every theme)
            # run for many minutes as one test, and a run starts them together, so a short
            # allowance fails all of them at once. A silent hang is stopped by run_watched.
            set -- "$@" -test-timeouts-enabled YES \
                -default-test-execution-time-allowance "${MONARCH_TEST_ALLOWANCE_SECONDS:-1800}" \
                -maximum-test-execution-time-allowance "${MONARCH_TEST_MAX_SECONDS:-3600}"
            ;;
    esac
    if [[ ${#runner[@]} -gt 0 ]]; then
        "${runner[@]}" xcodebuild "$@" || status=$?
    else
        xcodebuild "$@" >"$log" 2>&1 || status=$?
    fi
    if [[ "$status" -eq 124 ]]; then
        report_restarts "$log"
        return 1
    fi
    if [[ "$status" -ne 0 ]]; then
        report_restarts "$log"
        grep -E "✘|recorded an issue|\*\* .* \*\*" "$log" | tail -n 40 || true
        print_build_failure "$log" xcodebuild
        if grep -q "Restarting after unexpected exit, crash, or test timeout" "$log"; then
            echo "verify: the test runner restarted; the cause is in the block marked 'the test runner restarted' above." >&2
        fi
        return 1
    fi
    grep -E "✔ Test run|\*\* .* \*\*" "$log" || true
}

# Host tests (swift test) under run_watched; prints the result lines.
host_tests() {
    local log=.build/host-test.log status=0
    # MonarchUISnapshotTests compiles copies of the component test files (for their fixtures);
    # its own suites run only on the simulator (--snapshots), never on the host.
    run_watched "$log" swift test --skip-build --skip MonarchUISnapshotTests "${swift_flags[@]}" "$@" || status=$?
    grep -E "$results" "$log" || true
    if [[ "$status" -ne 0 && "$status" -ne 124 ]]; then
        echo "verify: host tests failed. Full log: $PWD/$log" >&2
    fi
    return "$status"
}

# Builds the package and its test targets for the iOS Simulator without running
# anything. Test code under #if os(iOS) (for example a test that needs
# @MainActor) is compiled only here, never by the host build.
simulator_compile() {
    step "Package and tests compiled for the iOS Simulator (build-for-testing, nothing is run)"
    run_xcodebuild .build/verify-build-for-testing.log build-for-testing -scheme Monarch-Package \
        -destination 'generic/platform=iOS Simulator' -derivedDataPath "$derived" \
        CODE_SIGNING_ALLOWED=NO SWIFT_TREAT_WARNINGS_AS_ERRORS=YES
}

# Shuts down, on exit, every simulator this run boots; ones already running stay.
shut_down_simulators_on_exit() {
    booted_before="$(xcrun simctl list devices booted | grep -Eo '[0-9A-F-]{36}' | sort || true)"
    trap shutdown_new_simulators EXIT
}
shutdown_new_simulators() {
    local booted_after
    booted_after="$(xcrun simctl list devices booted | grep -Eo '[0-9A-F-]{36}' | sort || true)"
    for udid in $(comm -13 <(echo "$booted_before") <(echo "$booted_after")); do
        xcrun simctl shutdown "$udid" || true
    done
}

# Boots the test destination and waits until it has finished booting. Started cold by
# xcodebuild, a simulator on a hosted runner can take longer to boot than the test runner
# waits ("The test runner timed out while preparing to run tests"). Skipped when the
# destination is not a named simulator; xcodebuild then boots it as before.
boot_destination() {
    local name udid
    name="$(sed -n 's/.*name=\([^,]*\).*/\1/p' <<<"$destination")"
    [[ -n "$name" ]] || return 0
    udid="$(xcrun simctl list devices available -j | python3 -c '
import json, sys
name = sys.argv[1]
runtimes = json.load(sys.stdin)["devices"]
for runtime in sorted((r for r in runtimes if ".iOS-" in r), key=lambda r: [int(x) for x in r.rsplit("iOS-", 1)[1].split("-")], reverse=True):
    for device in runtimes[runtime]:
        if device["name"] == name:
            print(device["udid"])
            raise SystemExit
' "$name")"
    [[ -n "$udid" ]] || return 0
    step "Booting $name ($udid)"
    xcrun simctl bootstatus "$udid" -b
    # Foundation formatters inside components follow the device's region, so the simulator gets the
    # same region on every machine (the hosted runners use en_US); a Mac set to another region would
    # otherwise record numbers and dates that CI draws differently.
    xcrun simctl spawn "$udid" defaults write -g AppleLocale -string en_US
    xcrun simctl spawn "$udid" defaults write -g AppleLanguages -array en-US
}

build_sample() {
    step "Sample app build for the iOS Simulator"
    xcodegen_bin="$(command -v xcodegen || true)"
    if [[ -z "$xcodegen_bin" ]]; then
        echo "verify: xcodegen is not installed; cannot generate the sample project" >&2
        exit 1
    fi
    "$xcodegen_bin" generate --quiet --spec Sample/project.yml
    run_xcodebuild .build/verify-sample.log build -project Sample/MonarchCatalogue.xcodeproj \
        -scheme MonarchCatalogue -destination 'generic/platform=iOS Simulator' \
        -derivedDataPath .build/xcode-sample CODE_SIGNING_ALLOWED=NO SWIFT_TREAT_WARNINGS_AS_ERRORS=YES
}

if [[ "$mode" == sample ]]; then
    step "Generated files (rewritten)"
    Scripts/generate.py
    build_sample
    printf '\nverify: the sample builds\n'
    exit 0
fi

# The screenshot tests on the simulator. Needs shut_down_simulators_on_exit.
run_snapshots() {
    boot_destination
    if [[ ! -d Tests/MonarchUISnapshotTests ]]; then
        echo "verify: Tests/MonarchUISnapshotTests does not exist yet" >&2
        exit 1
    fi
    step "Screenshot tests on: $destination"
    # xcodebuild passes TEST_RUNNER_<name> to the test process as <name>.
    if [[ -n "${MONARCH_SNAPSHOT_RECORD:-}" && "${MONARCH_SNAPSHOT_RECORD}" != 0 ]]; then export TEST_RUNNER_MONARCH_SNAPSHOT_RECORD=1; fi
    if [[ -n "$snapshot_ids" ]]; then export TEST_RUNNER_MONARCH_COMPONENT="$snapshot_ids"; fi
    # The target also compiles the component test files (it needs their fixtures), so only its own
    # suites are run, one after the other (they are .serialized).
    run_xcodebuild .build/verify-snapshots.log test -scheme Monarch-Package \
        -destination "$destination" -derivedDataPath "$derived" \
        -only-testing:MonarchUISnapshotTests/SnapshotTests \
        -only-testing:MonarchUISnapshotTests/SnapshotHarnessTests \
        -only-testing:MonarchUISnapshotTests/SnapshotClockTests \
        CODE_SIGNING_ALLOWED=NO SWIFT_TREAT_WARNINGS_AS_ERRORS=YES
}

if [[ "$mode" == snapshots ]]; then
    step "Generated files (rewritten)"
    Scripts/generate.py
    shut_down_simulators_on_exit
    run_snapshots
    printf '\nverify: the screenshot tests passed\n'
    exit 0
fi

if [[ "$mode" == component ]]; then
    step "Generated files"
    Scripts/generate.py
    ids="$(Scripts/generate.py --resolve "$component")"
    suites="$(Scripts/generate.py --suites "$component" | paste -sd '|' -)"
    files=()
    while IFS= read -r file; do [[ -n "$file" ]] && files+=("$file"); done < <(Scripts/generate.py --files "$component")
    echo "component: $component  ids: $ids  suites: ${suites:-none}"

    if [[ ${#files[@]} -gt 0 ]]; then
        step "Lint (${#files[@]} files of this component)"
        Scripts/lint.sh "${files[@]}"
    fi

    step "Host build, warnings as errors"
    host_build

    step "Tests: ${suites:-no suite of its own}, matrix fixtures and compiled examples of $ids"
    filter="ComponentMatrixTests|InventoryExampleTests${suites:+|$suites}"
    MONARCH_COMPONENT="$ids" host_tests --filter "$filter"

    simulator_compile

    printf '\nverify: %s passed the fast loop. Run Scripts/verify.sh --local before handing over.\n' "$component"
    exit 0
fi

step "Toolchain"
echo "DEVELOPER_DIR=$DEVELOPER_DIR"
xcodebuild -version
swift --version 2>&1 | head -n 1
echo "swift-format $(swift format --version)"

if [[ "$mode" == local ]]; then
    step "Generated files (rewritten)"
    Scripts/generate.py
else
    step "Inventory is valid and generated files are up to date"
    Scripts/generate.py --check
fi

step "Lint (swift-format, strict)"
Scripts/lint.sh

step "Host build, warnings as errors"
host_build

step "Host tests"
host_tests

if [[ "$mode" == local ]]; then
    printf '\nverify: local gates passed (API baseline and simulator gates are run at merge)\n'
    exit 0
fi

step "Public API matches the baseline"
Scripts/api.sh

if [[ "$mode" == host ]]; then
    simulator_compile
    printf '\nverify: host gates passed (simulator tests, DocC and sample skipped)\n'
    exit 0
fi

shut_down_simulators_on_exit

simulator_compile

boot_destination

step "Package tests on: $destination"
run_xcodebuild .build/verify-test.log test -scheme Monarch-Package \
    -destination "$destination" -derivedDataPath "$derived" \
    -skip-testing:MonarchUISnapshotTests CODE_SIGNING_ALLOWED=NO SWIFT_TREAT_WARNINGS_AS_ERRORS=YES

if [[ -d Tests/MonarchUISnapshotTests ]]; then
    snapshot_ids=""
    run_snapshots
fi

step "Documentation (DocC)"
run_xcodebuild .build/verify-docs.log docbuild -scheme MonarchUI \
    -destination 'generic/platform=iOS Simulator' -derivedDataPath "$derived" \
    CODE_SIGNING_ALLOWED=NO
if grep "warning:" .build/verify-docs.log | grep -qv appintentsmetadataprocessor; then
    grep "warning:" .build/verify-docs.log | grep -v appintentsmetadataprocessor | head -n 20
    echo "verify: the documentation build has warnings" >&2
    exit 1
fi

build_sample

printf '\nverify: all gates passed\n'
