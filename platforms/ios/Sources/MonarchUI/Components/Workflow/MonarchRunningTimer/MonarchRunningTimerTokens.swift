import Foundation
import MonarchTokens
import SwiftUI

/// What a running timer is busy with.
enum MonarchRunningTimerAction: Sendable, Hashable {
    case start
    case stop
    case stopAt
    case discard
}

/// Component tokens and pure logic for ``MonarchRunningTimer``.
enum MonarchRunningTimerTokens {
    /// The space between the parts of the panel.
    static let panelSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding of the panel.
    static let panelPadding: CGFloat = MonarchTokens.Space.space3
    /// The corner radius of the panel.
    static let panelCornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The space between the buttons of the panel.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The space between the icon, label and clock of the indicator.
    static let indicatorSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The padding before and after the parts of the indicator.
    static let indicatorPadding: CGFloat = MonarchTokens.Space.space2
    /// The space between the parts of the earlier-stop sheet.
    static let sheetSpacing: CGFloat = MonarchTokens.Space.space4
    /// The padding of the earlier-stop sheet.
    static let sheetPadding: CGFloat = MonarchTokens.Space.space6
    /// The space between an icon and the text of a message.
    static let messageSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The type role of the panel's clock.
    static let clockStyle: MonarchTextStyle = .title
    /// The type role of the indicator and of the panel's label.
    static let smallStyle: MonarchTextStyle = .caption
    /// The default warning time: 12 hours, as on the web.
    static let defaultWarnAfter: TimeInterval = 12 * secondsPerHour
    /// How often a running clock is redrawn, in seconds.
    static let tick: TimeInterval = 1
    /// The number of seconds in a minute.
    static let secondsPerMinute: TimeInterval = 60
    /// The number of seconds in an hour.
    static let secondsPerHour: TimeInterval = 3600
    /// The shortest time that can be saved with "I stopped earlier".
    static let shortestEntry: TimeInterval = secondsPerMinute
    /// How far after the start the earlier-stop sheet suggests, at most.
    static let suggestedEntry: TimeInterval = secondsPerHour
    /// What separates the parts of spoken text.
    static let spokenSeparator = ", "

    /// The shape of the panel.
    static var panelShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: panelCornerRadius, style: .continuous)
    }

    // MARK: Time

    /// Whole seconds since the start, never negative.
    static func elapsedSeconds(startedAt: Date, now: Date) -> Int {
        let interval = now.timeIntervalSince(startedAt)
        guard interval.isFinite, interval > 0 else { return 0 }
        return Int(interval.rounded(.down))
    }

    /// Whole seconds as a stopwatch: "0:04:09", "12:30:00".
    static func clockText(_ seconds: Int) -> String {
        let value = max(0, seconds)
        let hours = value / 3600
        let minutes = (value % 3600) / 60
        return String(format: "%d:%02d:%02d", hours, minutes, value % 60)
    }

    /// Whole seconds in words, for VoiceOver: "4 minutes, 9 seconds".
    static func spokenClock(_ seconds: Int, locale: Locale = .autoupdatingCurrent) -> String {
        let value = max(0, seconds)
        let units: Set<Duration.UnitsFormatStyle.Unit> = value == 0 ? [.seconds] : [.hours, .minutes, .seconds]
        let style = Duration.UnitsFormatStyle(
            allowedUnits: units,
            width: .wide,
            zeroValueUnits: value == 0 ? .show(length: 1) : .hide
        )
        return Duration.seconds(value).formatted(style.locale(locale))
    }

    /// Whole minutes as hours and minutes, as on the web: "2h 05m".
    static func hoursMinutes(_ interval: TimeInterval) -> String {
        let minutes = max(0, Int((interval / secondsPerMinute).rounded()))
        return String(format: "%dh %02dm", minutes / 60, minutes % 60)
    }

    /// The warning time as text: "12h", or "1h 30m" when it has minutes.
    static func warningDuration(_ interval: TimeInterval) -> String {
        let text = hoursMinutes(interval)
        return text.hasSuffix(" 00m") ? String(text.dropLast(4)) : text
    }

    /// Whether the timer has run longer than the warning time.
    static func isLongRunning(startedAt: Date?, now: Date, warnAfter: TimeInterval?) -> Bool {
        guard let startedAt, let warnAfter, warnAfter.isFinite else { return false }
        return now.timeIntervalSince(startedAt) > warnAfter
    }

    /// Whether the clock follows the time by itself: only a running timer
    /// without a fixed reference time does.
    static func isTicking(startedAt: Date?, now: Date?) -> Bool {
        startedAt != nil && now == nil
    }

    // MARK: Stopping earlier

    /// The times the earlier-stop picker offers: from the start to now.
    static func stopRange(startedAt: Date, now: Date) -> ClosedRange<Date> {
        startedAt...max(startedAt, now)
    }

    /// The time the earlier-stop sheet suggests: an hour after the start,
    /// or now if that is sooner.
    static func suggestedStop(startedAt: Date, now: Date) -> Date {
        min(max(startedAt, now), startedAt.addingTimeInterval(suggestedEntry))
    }

    /// Why a stop time cannot be saved, or `nil` when it can: it is in the
    /// future, or less than a minute after the start.
    static func stopError(
        _ stoppedAt: Date,
        startedAt: Date,
        now: Date,
        labels: MonarchRunningTimerLabels
    ) -> String? {
        if stoppedAt > now { return MonarchRunningTimerStrings.stoppedAtFuture(labels) }
        if stoppedAt.timeIntervalSince(startedAt) < shortestEntry {
            return MonarchRunningTimerStrings.stoppedAtTooEarly(labels)
        }
        return nil
    }

    /// The start as text for the earlier-stop sheet.
    static func startText(
        _ startedAt: Date,
        locale: Locale = .autoupdatingCurrent,
        timeZone: TimeZone = .autoupdatingCurrent
    ) -> String {
        var style = Date.FormatStyle(date: .abbreviated, time: .omitted).monarchTime()
        style.locale = locale
        style.timeZone = timeZone
        return startedAt.formatted(style)
    }

    // MARK: Messages

    /// The message of a failed action: the error's localized description,
    /// or the built-in "That did not work. Try again.".
    static func failureMessage(for error: any Error, labels: MonarchRunningTimerLabels) -> String {
        MonarchFailureMessage.message(for: error, fallback: MonarchRunningTimerStrings.failed(labels))
    }

    /// The name of the state, read and announced.
    static func stateText(isRunning: Bool, labels: MonarchRunningTimerLabels) -> String {
        isRunning ? MonarchRunningTimerStrings.running(labels) : MonarchRunningTimerStrings.stopped(labels)
    }

    // MARK: Appearance

    /// The colour roles of the indicator: accent while running, warning
    /// once it has run too long, danger after a failed action, plain while
    /// stopped.
    static func indicatorAppearance(isRunning: Bool, isLongRunning: Bool, hasFailed: Bool) -> MonarchSurfaceAppearance {
        if hasFailed {
            return MonarchSurfaceAppearance(
                background: \.dangerSubtle,
                foreground: \.dangerStrong,
                border: \.dangerBorder
            )
        }
        if isRunning, isLongRunning {
            return MonarchSurfaceAppearance(
                background: \.warningSubtle,
                foreground: \.warningStrong,
                border: \.warningBorder
            )
        }
        if isRunning {
            return MonarchSurfaceAppearance(
                background: \.accentSubtle,
                foreground: \.accentStrong,
                border: \.accentBorder
            )
        }
        return MonarchSurfaceAppearance(background: \.bgSurface, foreground: \.textDefault, border: \.borderDefault)
    }

    /// The SF Symbol before the indicator's label.
    static func indicatorSymbol(isLongRunning: Bool, hasFailed: Bool) -> String {
        if hasFailed { return "exclamationmark.circle" }
        return isLongRunning ? "exclamationmark.triangle" : "clock"
    }

    /// The colour role of the panel's clock: accent while running, muted
    /// while stopped.
    static func clockForeground(isRunning: Bool) -> MonarchColorRole {
        isRunning ? \.accentStrong : \.textMuted
    }

    /// The colour roles of the panel.
    static var panelAppearance: MonarchSurfaceAppearance {
        MonarchSurfaceAppearance(background: \.bgSurface, foreground: \.textDefault, border: \.borderSubtle)
    }

    // MARK: Accessibility

    /// What VoiceOver reads for the clock: the label, if any, and the time
    /// in words.
    static func clockAccessibilityLabel(label: String?, seconds: Int, locale: Locale = .autoupdatingCurrent) -> String {
        [label, spokenClock(seconds, locale: locale)]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: spokenSeparator)
    }

    /// What VoiceOver reads as the value of the clock: the state, then a
    /// failure if there is one.
    static func clockAccessibilityValue(isRunning: Bool, failure: String?, labels: MonarchRunningTimerLabels) -> String
    {
        [stateText(isRunning: isRunning, labels: labels), failure]
            .compactMap { $0 }
            .joined(separator: spokenSeparator)
    }
}
