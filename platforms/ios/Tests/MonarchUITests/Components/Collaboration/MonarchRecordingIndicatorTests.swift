import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRecordingIndicatorFixtures: ComponentFixtures {
    static let id = "recording-indicator"
    static let fixtures = [
        Fixture("recording, both variants") {
            VStack(alignment: .leading) {
                ForEach(MonarchRecordingIndicatorVariant.allCases, id: \.self) { variant in
                    MonarchRecordingIndicator(isRecording: true, variant: variant)
                }
            }
        },
        Fixture("with elapsed time and stop button") {
            MonarchRecordingIndicator(isRecording: true, elapsed: 252, onStop: {})
        },
        Fixture("ticking from a start date, over video") {
            MonarchRecordingIndicator(
                isRecording: true,
                since: Date(timeIntervalSinceNow: -3723),
                variant: .solid,
                onStop: {}
            )
        },
        Fixture("paused, with custom strings") {
            VStack(alignment: .leading) {
                MonarchRecordingIndicator(isRecording: true, elapsed: 59, isPaused: true)
                MonarchRecordingIndicator(
                    isRecording: true,
                    elapsed: 59,
                    isPaused: true,
                    labels: MonarchRecordingIndicatorLabels(paused: "Aufnahme pausiert")
                )
            }
        },
        Fixture("not recording, beside a title") {
            HStack {
                Text("Weekly sync")
                MonarchRecordingIndicator(isRecording: false, since: .now)
            }
        },
    ]
}

@Suite("MonarchRecordingIndicator")
struct MonarchRecordingIndicatorTests {
    typealias Tokens = MonarchRecordingIndicatorTokens
    typealias Phase = MonarchRecordingIndicatorTokens.Phase

    private let labels = MonarchRecordingIndicatorLabels()
    private let english = Locale(identifier: "en_US")

    @Test("Starting and stopping are announced")
    func startAndStop() {
        let off = Phase(isRecording: false, isPaused: false)
        let on = Phase(isRecording: true, isPaused: false)
        #expect(Tokens.announcement(from: off, to: on, labels: labels) == "Recording started")
        #expect(Tokens.announcement(from: on, to: off, labels: labels) == "Recording stopped")
        #expect(Tokens.announcement(from: on, to: on, labels: labels) == nil)
    }

    @Test("Pausing and resuming are announced only while recording")
    func pauseAndResume() {
        let on = Phase(isRecording: true, isPaused: false)
        let paused = Phase(isRecording: true, isPaused: true)
        #expect(Tokens.announcement(from: on, to: paused, labels: labels) == "Recording paused")
        #expect(Tokens.announcement(from: paused, to: on, labels: labels) == "Recording resumed")
        let off = Phase(isRecording: false, isPaused: false)
        let offPaused = Phase(isRecording: false, isPaused: true)
        #expect(Tokens.announcement(from: off, to: offPaused, labels: labels) == nil)
        #expect(Tokens.announcement(from: paused, to: off, labels: labels) == "Recording stopped")
    }

    @Test("Replacement strings are used for announcements, text and labels")
    func overrides() {
        let custom = MonarchRecordingIndicatorLabels(
            rec: "AUFN",
            recording: "Aufnahme",
            paused: "Pausiert",
            started: "Aufnahme gestartet"
        )
        let off = Phase(isRecording: false, isPaused: false)
        let on = Phase(isRecording: true, isPaused: false)
        #expect(Tokens.announcement(from: off, to: on, labels: custom) == "Aufnahme gestartet")
        #expect(Tokens.visibleText(isPaused: false, labels: custom) == "AUFN")
        #expect(Tokens.visibleText(isPaused: true, labels: custom) == "Pausiert")
        #expect(Tokens.accessibilityLabel(isPaused: false, labels: custom) == "Aufnahme")
    }

    @Test("VoiceOver reads Recording, not the short visible text")
    func accessibilityLabel() {
        #expect(Tokens.visibleText(isPaused: false, labels: labels) == "REC")
        #expect(Tokens.visibleText(isPaused: true, labels: labels) == "Paused")
        #expect(Tokens.accessibilityLabel(isPaused: false, labels: labels) == "Recording")
        #expect(Tokens.accessibilityLabel(isPaused: true, labels: labels) == "Recording, Paused")
    }

    @Test("The time is shown when a time is known, unless asked otherwise")
    func showsTime() {
        #expect(!Tokens.showsTime(showsElapsed: nil, since: nil, elapsed: nil))
        #expect(Tokens.showsTime(showsElapsed: nil, since: .now, elapsed: nil))
        #expect(Tokens.showsTime(showsElapsed: nil, since: nil, elapsed: 3))
        #expect(!Tokens.showsTime(showsElapsed: false, since: .now, elapsed: 3))
        #expect(Tokens.showsTime(showsElapsed: true, since: nil, elapsed: nil))
    }

    @Test("The time advances only from a start date, while not paused")
    func ticking() {
        #expect(Tokens.isTicking(isPaused: false, since: .now, elapsed: nil))
        #expect(!Tokens.isTicking(isPaused: true, since: .now, elapsed: nil))
        #expect(!Tokens.isTicking(isPaused: false, since: .now, elapsed: 10))
        #expect(!Tokens.isTicking(isPaused: false, since: nil, elapsed: nil))
    }

    @Test("Elapsed takes precedence over since, and the time is never negative")
    func seconds() {
        let now = Date(timeIntervalSinceReferenceDate: 1000)
        let start = Date(timeIntervalSinceReferenceDate: 748.9)
        #expect(Tokens.seconds(since: start, elapsed: nil, now: now) == 251)
        #expect(Tokens.seconds(since: start, elapsed: 30, now: now) == 30)
        #expect(Tokens.seconds(since: now.addingTimeInterval(60), elapsed: nil, now: now) == 0)
        #expect(Tokens.seconds(since: nil, elapsed: -5, now: now) == 0)
        #expect(Tokens.seconds(since: nil, elapsed: .nan, now: now) == 0)
        #expect(Tokens.seconds(since: nil, elapsed: nil, now: now) == 0)
    }

    @Test("The time is written as minutes and seconds, with hours from one hour")
    func timeText() {
        #expect(Tokens.timeText(seconds: 0, locale: english) == "00:00")
        #expect(Tokens.timeText(seconds: 252, locale: english) == "04:12")
        #expect(Tokens.timeText(seconds: 3599, locale: english) == "59:59")
        #expect(Tokens.timeText(seconds: 3723, locale: english) == "1:02:03")
    }

    @Test("The time is spoken in words")
    func spokenTime() {
        let spoken = Tokens.spokenTime(seconds: 252, locale: english)
        #expect(spoken.contains("4 minutes"))
        #expect(spoken.contains("12 seconds"))
        #expect(!spoken.contains("hour"))
        #expect(Tokens.spokenTime(seconds: 3723, locale: english).contains("1 hour"))
    }

    @Test("Colours follow the variant")
    func appearance() {
        let subtle = Tokens.appearance(variant: .subtle)
        #expect(subtle.background == \MonarchColorTokens.dangerSubtle)
        #expect(subtle.foreground == \MonarchColorTokens.dangerStrong)
        #expect(subtle.border == \MonarchColorTokens.dangerBorder)
        let solid = Tokens.appearance(variant: .solid)
        #expect(solid.background == \MonarchColorTokens.bgInverse)
        #expect(solid.foreground == \MonarchColorTokens.textInverse)
        #expect(solid.border == nil)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchRecordingIndicatorStrings.rec == "REC")
        #expect(MonarchRecordingIndicatorStrings.recording == "Recording")
        #expect(MonarchRecordingIndicatorStrings.paused == "Paused")
        #expect(MonarchRecordingIndicatorStrings.started == "Recording started")
        #expect(MonarchRecordingIndicatorStrings.stopped == "Recording stopped")
        #expect(MonarchRecordingIndicatorStrings.pausedAnnouncement == "Recording paused")
        #expect(MonarchRecordingIndicatorStrings.resumed == "Recording resumed")
        #expect(MonarchRecordingIndicatorStrings.stop == "Stop recording")
    }

    @Test("Nothing is drawn while not recording, and the stop button is a touch target")
    @MainActor
    func layout() {
        let idle = Host.layout(MonarchRecordingIndicator(isRecording: false, elapsed: 10, onStop: {}))
        let active = Host.layout(MonarchRecordingIndicator(isRecording: true, elapsed: 10, onStop: {}))
        #expect(idle.height < 1)
        #expect(active.height >= MonarchAccessibility.minimumHitTarget)
    }
}
