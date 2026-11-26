import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRunningTimerFixtures: ComponentFixtures {
    static let id = "running-timer"
    private static let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

    static let fixtures = [
        Fixture("panel, stopped, with Start") {
            VStack {
                MonarchRunningTimer(startedAt: nil, onStart: {})
                MonarchRunningTimer(startedAt: nil, isStartDisabled: true, onStart: {})
                MonarchRunningTimer(startedAt: nil, isPending: true, onStart: {})
            }
        },
        Fixture("panel, running, every action") {
            MonarchRunningTimer(
                startedAt: now.addingTimeInterval(-249),
                label: "ENG-142 Fix the export of large reports",
                now: now,
                onStop: {},
                onStopAt: { _ in },
                onDiscard: {}
            )
        },
        Fixture("panel, running too long, with fields") {
            MonarchRunningTimer(
                startedAt: now.addingTimeInterval(-50_000),
                label: "Design review",
                now: now,
                onStop: {},
                fields: {
                    Text("Project: Atlas")
                        .monarchTextStyle(.body)
                }
            )
        },
        Fixture("panel, live clock") {
            MonarchRunningTimer(startedAt: Date(timeIntervalSinceNow: -3725), onStop: {})
        },
        Fixture("indicator, running, with open, stop and menu") {
            MonarchRunningTimer(
                startedAt: now.addingTimeInterval(-249),
                label: "ENG-142",
                variant: .indicator,
                now: now,
                onStop: {},
                onStopAt: { _ in },
                onDiscard: {},
                onOpen: {}
            )
        },
        Fixture("indicator, long running, without actions") {
            MonarchRunningTimer(
                startedAt: now.addingTimeInterval(-50_000),
                label: "A very long label that does not fit in a toolbar",
                variant: .indicator,
                now: now
            )
        },
        Fixture("indicator, stopped, shown and hidden") {
            VStack(alignment: .leading) {
                MonarchRunningTimer(startedAt: nil, variant: .indicator, showsWhenStopped: true)
                MonarchRunningTimer(startedAt: nil, variant: .indicator)
                Text("The second indicator is hidden.")
            }
        },
        Fixture("custom strings, warning turned off") {
            MonarchRunningTimer(
                startedAt: now.addingTimeInterval(-50_000),
                warnAfter: nil,
                now: now,
                labels: MonarchRunningTimerLabels(stop: "Anhalten", stoppedEarlier: "Früher beendet"),
                onStop: {},
                onStopAt: { _ in }
            )
        },
    ]
}

private struct TimerFailure: LocalizedError {
    var errorDescription: String? { "The server is offline." }
}

private struct SilentFailure: Error {}

@Suite("MonarchRunningTimer")
struct MonarchRunningTimerTests {
    typealias Tokens = MonarchRunningTimerTokens

    private let labels = MonarchRunningTimerLabels()
    private let english = Locale(identifier: "en_US")
    private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

    // MARK: Time

    @Test("The clock is hours, minutes and seconds")
    func clockText() {
        #expect(Tokens.clockText(0) == "0:00:00")
        #expect(Tokens.clockText(249) == "0:04:09")
        #expect(Tokens.clockText(45_000) == "12:30:00")
        #expect(Tokens.clockText(-5) == "0:00:00")
        #expect(Tokens.clockText(360_000) == "100:00:00")
    }

    @Test("Elapsed time is whole seconds and never negative")
    func elapsed() {
        #expect(Tokens.elapsedSeconds(startedAt: now.addingTimeInterval(-249.9), now: now) == 249)
        #expect(Tokens.elapsedSeconds(startedAt: now.addingTimeInterval(30), now: now) == 0)
        #expect(Tokens.elapsedSeconds(startedAt: now, now: now) == 0)
    }

    @Test("The time is read in words")
    func spoken() {
        #expect(Tokens.spokenClock(249, locale: english) == "4 minutes, 9 seconds")
        #expect(Tokens.spokenClock(3600, locale: english) == "1 hour")
        #expect(Tokens.spokenClock(0, locale: english) == "0 seconds")
    }

    @Test("Durations are hours and minutes; the warning drops zero minutes")
    func durations() {
        #expect(Tokens.hoursMinutes(7500) == "2h 05m")
        #expect(Tokens.hoursMinutes(-60) == "0h 00m")
        #expect(Tokens.warningDuration(12 * 3600) == "12h")
        #expect(Tokens.warningDuration(5400) == "1h 30m")
    }

    @Test("A timer is long-running after the warning time, never without one")
    func longRunning() {
        let start = now.addingTimeInterval(-3601)
        #expect(Tokens.isLongRunning(startedAt: start, now: now, warnAfter: 3600))
        #expect(!Tokens.isLongRunning(startedAt: start, now: now, warnAfter: 7200))
        #expect(!Tokens.isLongRunning(startedAt: start, now: now, warnAfter: nil))
        #expect(!Tokens.isLongRunning(startedAt: nil, now: now, warnAfter: 1))
    }

    @Test("Only a running timer without a fixed time ticks")
    func ticking() {
        #expect(Tokens.isTicking(startedAt: now, now: nil))
        #expect(!Tokens.isTicking(startedAt: now, now: now))
        #expect(!Tokens.isTicking(startedAt: nil, now: nil))
    }

    @Test("The clock is redrawn on each whole second after the start")
    func schedule() {
        let start = now.addingTimeInterval(-10.25)
        var entries = MonarchRunningTimerSchedule(startedAt: start)
            .entries(from: now, mode: .normal)
            .makeIterator()
        #expect(entries.next() == now)
        #expect(entries.next() == now.addingTimeInterval(0.75))
        #expect(entries.next() == now.addingTimeInterval(1.75))
        var stopped = MonarchRunningTimerSchedule(startedAt: nil).entries(from: now, mode: .normal).makeIterator()
        #expect(stopped.next() == now)
        #expect(stopped.next() == nil)
    }

    // MARK: Stopping earlier

    @Test("The earlier stop is suggested an hour after the start, or now")
    func suggestion() {
        let start = now.addingTimeInterval(-7200)
        #expect(Tokens.suggestedStop(startedAt: start, now: now) == start.addingTimeInterval(3600))
        let recent = now.addingTimeInterval(-600)
        #expect(Tokens.suggestedStop(startedAt: recent, now: now) == now)
        #expect(Tokens.stopRange(startedAt: recent, now: now) == recent...now)
        #expect(Tokens.stopRange(startedAt: now, now: recent) == now...now)
    }

    @Test("A stop time in the future or within a minute of the start is refused")
    func stopErrors() {
        let start = now.addingTimeInterval(-3600)
        #expect(Tokens.stopError(now.addingTimeInterval(60), startedAt: start, now: now, labels: labels) != nil)
        #expect(Tokens.stopError(start.addingTimeInterval(59), startedAt: start, now: now, labels: labels) != nil)
        #expect(Tokens.stopError(start.addingTimeInterval(60), startedAt: start, now: now, labels: labels) == nil)
        #expect(Tokens.stopError(now, startedAt: start, now: now, labels: labels) == nil)
        let custom = MonarchRunningTimerLabels(stoppedAtFuture: "Zukunft")
        #expect(Tokens.stopError(now.addingTimeInterval(1), startedAt: start, now: now, labels: custom) == "Zukunft")
    }

    // MARK: Messages

    @Test("A failure shows the error's description, or the built-in message")
    func failures() {
        #expect(Tokens.failureMessage(for: TimerFailure(), labels: labels) == "The server is offline.")
        #expect(Tokens.failureMessage(for: SilentFailure(), labels: labels) == "That did not work. Try again.")
        let custom = MonarchRunningTimerLabels(failed: "Fehler")
        #expect(Tokens.failureMessage(for: SilentFailure(), labels: custom) == "Fehler")
    }

    // MARK: Appearance

    @Test("The indicator is accent while running, warning when long, danger after a failure")
    func appearance() {
        let running = Tokens.indicatorAppearance(isRunning: true, isLongRunning: false, hasFailed: false)
        #expect(running.background == \MonarchColorTokens.accentSubtle)
        #expect(running.foreground == \MonarchColorTokens.accentStrong)
        let long = Tokens.indicatorAppearance(isRunning: true, isLongRunning: true, hasFailed: false)
        #expect(long.foreground == \MonarchColorTokens.warningStrong)
        let failed = Tokens.indicatorAppearance(isRunning: true, isLongRunning: true, hasFailed: true)
        #expect(failed.foreground == \MonarchColorTokens.dangerStrong)
        let stopped = Tokens.indicatorAppearance(isRunning: false, isLongRunning: true, hasFailed: false)
        #expect(stopped.foreground == \MonarchColorTokens.textDefault)
        #expect(stopped.border == \MonarchColorTokens.borderDefault)
        #expect(Tokens.clockForeground(isRunning: true) == \MonarchColorTokens.accentStrong)
        #expect(Tokens.clockForeground(isRunning: false) == \MonarchColorTokens.textMuted)
    }

    @Test("Every state of the indicator has its own symbol")
    func symbols() {
        let symbols = [
            Tokens.indicatorSymbol(isLongRunning: false, hasFailed: false),
            Tokens.indicatorSymbol(isLongRunning: true, hasFailed: false),
            Tokens.indicatorSymbol(isLongRunning: false, hasFailed: true),
        ]
        #expect(Set(symbols).count == 3)
    }

    // MARK: Accessibility

    @Test("The clock is read as the label and the time, with the state as the value")
    func accessibility() {
        #expect(
            Tokens.clockAccessibilityLabel(label: "ENG-142", seconds: 249, locale: english)
                == "ENG-142, 4 minutes, 9 seconds"
        )
        #expect(Tokens.clockAccessibilityLabel(label: nil, seconds: 0, locale: english) == "0 seconds")
        #expect(Tokens.clockAccessibilityValue(isRunning: true, failure: nil, labels: labels) == "Timer running")
        #expect(
            Tokens.clockAccessibilityValue(isRunning: false, failure: "Offline", labels: labels)
                == "Timer stopped, Offline"
        )
    }

    @Test("Every built-in string resolves, and labels replace them")
    func strings() {
        let all = [
            MonarchRunningTimerStrings.start(labels),
            MonarchRunningTimerStrings.stop(labels),
            MonarchRunningTimerStrings.stopAndLog(labels),
            MonarchRunningTimerStrings.moreActions(labels),
            MonarchRunningTimerStrings.discard(labels),
            MonarchRunningTimerStrings.discardTitle(labels),
            MonarchRunningTimerStrings.discardMessage(labels),
            MonarchRunningTimerStrings.discardConfirm(labels),
            MonarchRunningTimerStrings.stoppedEarlier(labels),
            MonarchRunningTimerStrings.stoppedEarlierTitle(labels),
            MonarchRunningTimerStrings.stoppedEarlierMessage(labels),
            MonarchRunningTimerStrings.stoppedAt(labels),
            MonarchRunningTimerStrings.started("today", labels),
            MonarchRunningTimerStrings.workDuration("2h 05m", labels),
            MonarchRunningTimerStrings.save(labels),
            MonarchRunningTimerStrings.cancel(labels),
            MonarchRunningTimerStrings.stoppedAtFuture(labels),
            MonarchRunningTimerStrings.stoppedAtTooEarly(labels),
            MonarchRunningTimerStrings.failed(labels),
            MonarchRunningTimerStrings.running(labels),
            MonarchRunningTimerStrings.stopped(labels),
            MonarchRunningTimerStrings.defaultLabel(labels),
            MonarchRunningTimerStrings.longRunning("12h", labels),
            MonarchRunningTimerStrings.openHint(labels),
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
        #expect(MonarchRunningTimerStrings.started("today", labels) == "Started today")
        #expect(MonarchRunningTimerStrings.workDuration("2h 05m", labels) == "That is 2h 05m of work.")
        let custom = MonarchRunningTimerLabels(start: "Los", longRunning: { "über \($0)" })
        #expect(MonarchRunningTimerStrings.start(custom) == "Los")
        #expect(MonarchRunningTimerStrings.longRunning("12h", custom) == "über 12h")
    }

    #if os(iOS)
        @MainActor
        @Test("The indicator's buttons have 44-point touch targets")
        func touchTargets() {
            let size = Host.layout(
                MonarchRunningTimer(
                    startedAt: now.addingTimeInterval(-60),
                    variant: .indicator,
                    now: now,
                    onStop: {},
                    onDiscard: {}
                )
                .fixedSize()
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
