import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchRecordingIndicator``.
enum MonarchRecordingIndicatorTokens {
    /// Padding before and after the content of the capsule.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below the content of the capsule.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the dot, the text and the time.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the capsule and the stop button.
    static let stopSpacing: CGFloat = MonarchTokens.Space.space2
    /// The diameter of the dot before Dynamic Type is applied.
    static let dotSize: CGFloat = MonarchTokens.Space.space2
    /// The scale the dot shrinks to while it pulses.
    static let dotRestScale: CGFloat = 0.6
    /// How long the dot takes to shrink or grow, in seconds.
    static let pulseDuration: TimeInterval = MonarchPulse.period / 2
    /// How often the time is redrawn, in seconds.
    static let tick: TimeInterval = 1
    /// The number of seconds from which the time shows hours.
    static let secondsPerHour: TimeInterval = 3600
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .caption
    /// The weight of the text.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The SF Symbol shown instead of the dot while paused.
    static let pausedSymbol = "pause.fill"
    /// The SF Symbol of the stop button.
    static let stopSymbol = "stop.fill"
    /// What separates the parts of a spoken label. VoiceOver pauses at it.
    static let spokenSeparator = ", "

    /// The role of the dot.
    static var dot: MonarchColorRole { \.dangerSolid }

    /// The surface roles of the capsule for a variant.
    static func appearance(variant: MonarchRecordingIndicatorVariant) -> MonarchSurfaceAppearance {
        switch variant {
        case .subtle:
            MonarchSurfaceAppearance(background: \.dangerSubtle, foreground: \.dangerStrong, border: \.dangerBorder)
        case .solid:
            MonarchSurfaceAppearance(background: \.bgInverse, foreground: \.textInverse, border: nil)
        }
    }

    /// The recording state that is announced when it changes.
    struct Phase: Equatable {
        let isRecording: Bool
        let isPaused: Bool
    }

    /// What is announced when the state changes, or `nil` for nothing.
    /// Pausing and resuming are announced only while recording.
    static func announcement(
        from old: Phase,
        to new: Phase,
        labels: MonarchRecordingIndicatorLabels
    ) -> String? {
        if new.isRecording != old.isRecording {
            return new.isRecording
                ? labels.started ?? MonarchRecordingIndicatorStrings.started
                : labels.stopped ?? MonarchRecordingIndicatorStrings.stopped
        }
        guard new.isRecording, new.isPaused != old.isPaused else { return nil }
        return new.isPaused
            ? labels.pausedAnnouncement ?? MonarchRecordingIndicatorStrings.pausedAnnouncement
            : labels.resumed ?? MonarchRecordingIndicatorStrings.resumed
    }

    /// The visible text of the capsule.
    static func visibleText(isPaused: Bool, labels: MonarchRecordingIndicatorLabels) -> String {
        isPaused
            ? labels.paused ?? MonarchRecordingIndicatorStrings.paused
            : labels.rec ?? MonarchRecordingIndicatorStrings.rec
    }

    /// What VoiceOver reads as the label of the capsule: "Recording", or
    /// "Recording, Paused".
    static func accessibilityLabel(isPaused: Bool, labels: MonarchRecordingIndicatorLabels) -> String {
        let recording = labels.recording ?? MonarchRecordingIndicatorStrings.recording
        guard isPaused else { return recording }
        return [recording, labels.paused ?? MonarchRecordingIndicatorStrings.paused]
            .joined(separator: spokenSeparator)
    }

    /// Whether the time is shown: as asked, or else whenever a time is known.
    static func showsTime(showsElapsed: Bool?, since: Date?, elapsed: TimeInterval?) -> Bool {
        showsElapsed ?? (since != nil || elapsed != nil)
    }

    /// Whether the time advances by itself.
    static func isTicking(isPaused: Bool, since: Date?, elapsed: TimeInterval?) -> Bool {
        !isPaused && elapsed == nil && since != nil
    }

    /// The recorded time in whole seconds, never negative. `elapsed` takes
    /// precedence over `since`.
    static func seconds(since: Date?, elapsed: TimeInterval?, now: Date) -> Int64 {
        let interval = elapsed ?? since.map { now.timeIntervalSince($0) } ?? 0
        guard interval.isFinite, interval > 0 else { return 0 }
        return Int64(interval)
    }

    /// The time as digits, such as `04:12` or `1:02:03`.
    static func timeText(seconds: Int64, locale: Locale = .current) -> String {
        let pattern: Duration.TimeFormatStyle.Pattern =
            TimeInterval(seconds) >= secondsPerHour ? .hourMinuteSecond : .minuteSecond(padMinuteToLength: 2)
        return Duration.seconds(seconds).formatted(Duration.TimeFormatStyle(pattern: pattern, locale: locale))
    }

    /// The time in words, such as "4 minutes, 12 seconds", for VoiceOver.
    static func spokenTime(seconds: Int64, locale: Locale = .current) -> String {
        Duration.seconds(seconds).formatted(
            Duration.UnitsFormatStyle(
                allowedUnits: [.hours, .minutes, .seconds],
                width: .wide,
                zeroValueUnits: .hide
            )
            .locale(locale)
        )
    }
}
