import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSlaIndicator``.
enum MonarchSlaIndicatorTokens {
    /// Padding before and after the content of the chip.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// Padding above and below the content of the chip.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the icon and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The corner radius of the chip.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.sm
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .caption
    /// The length of the step in which the time left is shown, in seconds.
    static let tick: TimeInterval = 60
    /// The number of seconds in an hour.
    static let secondsPerHour: TimeInterval = 3600
    /// The number of seconds in a day.
    static let secondsPerDay: TimeInterval = 86400
    /// What separates the parts of spoken text. VoiceOver pauses at it.
    static let spokenSeparator = ", "

    /// The shape of the chip.
    static var chipShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
    }

    // MARK: State

    /// The state to show, from the state the server reported, the due time
    /// and the clock. A running target whose due time has passed is
    /// breached; one within `warnBefore` of its due time is at risk.
    /// Paused, met, breached and not-applicable targets are never changed.
    static func resolve(
        _ state: MonarchSlaIndicatorState,
        dueAt: Date?,
        now: Date,
        warnBefore: TimeInterval?
    ) -> MonarchSlaIndicatorState {
        guard state == .onTrack || state == .atRisk, let dueAt else { return state }
        let remaining = dueAt.timeIntervalSince(now)
        if remaining <= 0 { return .breached }
        if state == .onTrack, let warnBefore, remaining <= warnBefore { return .atRisk }
        return state
    }

    /// Whether the indicator follows the clock by itself: only a running
    /// target with a due time does.
    static func isTicking(state: MonarchSlaIndicatorState, dueAt: Date?, isLive: Bool, now: Date?) -> Bool {
        isLive && now == nil && dueAt != nil && (state == .onTrack || state == .atRisk)
    }

    /// The next moment after `date` at which the text or the state can
    /// change: the next whole minute before or after the due time, or the
    /// moment the target becomes at risk if that comes first.
    static func nextTick(after date: Date, dueAt: Date, warnBefore: TimeInterval?) -> Date {
        let steps = (date.timeIntervalSince(dueAt) / tick).rounded(.down) + 1
        var next = dueAt.addingTimeInterval(steps * tick)
        if next <= date { next = date.addingTimeInterval(tick) }
        if let warnBefore, warnBefore.isFinite {
            let edge = dueAt.addingTimeInterval(-warnBefore)
            if edge > date, edge < next { next = edge }
        }
        return next
    }

    // MARK: Appearance

    /// The SF Symbol of a state, or `nil` when there is no target.
    static func symbol(for state: MonarchSlaIndicatorState) -> String? {
        switch state {
        case .onTrack, .atRisk: "clock"
        case .breached: "xmark.circle"
        case .paused: "pause.circle"
        case .met: "checkmark.circle"
        case .notApplicable: nil
        }
    }

    /// The weight of the text: regular when there is no target, medium
    /// otherwise.
    static func fontWeight(for state: MonarchSlaIndicatorState) -> Font.Weight {
        MonarchTextStyleSpec.fontWeight(
            state == .notApplicable ? MonarchTokens.FontWeight.regular : MonarchTokens.FontWeight.medium
        )
    }

    /// The colour roles for a state and variant. A breached chip is solid
    /// danger, the loudest element of a list on purpose. In a high-contrast
    /// theme a chip without a border of its own gets a strong border.
    static func appearance(
        state: MonarchSlaIndicatorState,
        variant: MonarchSlaIndicatorVariant,
        isHighContrast: Bool
    ) -> MonarchSurfaceAppearance {
        let foreground: MonarchColorRole =
            switch state {
            case .onTrack, .paused: \.textMuted
            case .atRisk: \.warningStrong
            case .breached: \.dangerStrong
            case .met: \.successStrong
            case .notApplicable: \.textSubtle
            }
        guard variant == .chip else {
            return MonarchSurfaceAppearance(background: nil, foreground: foreground, border: nil)
        }
        let outline: MonarchColorRole? = isHighContrast ? \.borderStrong : nil
        return switch state {
        case .atRisk:
            MonarchSurfaceAppearance(background: \.warningSubtle, foreground: foreground, border: \.warningBorder)
        case .breached:
            MonarchSurfaceAppearance(background: \.dangerSolid, foreground: \.dangerOnSolid, border: nil)
        case .paused:
            MonarchSurfaceAppearance(background: \.bgMuted, foreground: foreground, border: outline)
        case .onTrack, .met, .notApplicable:
            MonarchSurfaceAppearance(background: nil, foreground: foreground, border: outline)
        }
    }

    // MARK: Text

    /// The name of a state.
    static func name(of state: MonarchSlaIndicatorState, labels: MonarchSlaIndicatorLabels) -> String {
        let override: String? =
            switch state {
            case .onTrack: labels.onTrack
            case .atRisk: labels.atRisk
            case .breached: labels.breached
            case .paused: labels.paused
            case .met: labels.met
            case .notApplicable: labels.notApplicable
            }
        return override ?? MonarchSlaIndicatorStrings.name(of: state)
    }

    /// A span of time as "<1m", "45m", "2h 15m" or "3d 4h": whole minutes,
    /// rounded down, in at most two units. With `isSpoken` the units are
    /// written out for VoiceOver, such as "2 hours, 15 minutes".
    static func durationText(
        _ interval: TimeInterval,
        isSpoken: Bool = false,
        labels: MonarchSlaIndicatorLabels = MonarchSlaIndicatorLabels(),
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        let seconds = interval.isFinite ? abs(interval) : 0
        if let duration = labels.duration { return duration(seconds) }
        guard seconds >= tick else {
            return isSpoken
                ? MonarchSlaIndicatorStrings.lessThanMinuteSpoken : MonarchSlaIndicatorStrings.lessThanMinute
        }
        // Days are shown with hours and hours with minutes. Rounding down to
        // the smaller unit first keeps the formatter from rounding up.
        let isDays = seconds >= secondsPerDay
        let unit = isDays ? secondsPerHour : tick
        let whole = (seconds / unit).rounded(.down) * unit
        let style = Duration.UnitsFormatStyle(
            allowedUnits: isDays ? [.days, .hours] : [.hours, .minutes],
            width: isSpoken ? .wide : .narrow,
            maximumUnitCount: 2,
            zeroValueUnits: .hide
        )
        return Duration.seconds(whole).formatted(style.locale(locale))
    }

    /// The time left or overdue, or `nil` when the state has no countdown:
    /// "2h 15m left" for a running target with a due time, "Overdue 40m"
    /// for a breached one whose due time has passed.
    static func timeText(
        state: MonarchSlaIndicatorState,
        dueAt: Date?,
        now: Date,
        isSpoken: Bool = false,
        labels: MonarchSlaIndicatorLabels,
        locale: Locale = .autoupdatingCurrent
    ) -> String? {
        guard let dueAt else { return nil }
        let remaining = dueAt.timeIntervalSince(now)
        switch state {
        case .breached:
            guard remaining < 0 else { return nil }
            let duration = durationText(-remaining, isSpoken: isSpoken, labels: labels, locale: locale)
            return labels.overdue?(duration) ?? MonarchSlaIndicatorStrings.overdue(duration)
        case .onTrack, .atRisk:
            let duration = durationText(remaining, isSpoken: isSpoken, labels: labels, locale: locale)
            return labels.remaining?(duration) ?? MonarchSlaIndicatorStrings.remaining(duration)
        case .paused, .met, .notApplicable:
            return nil
        }
    }

    /// The visible text: the time left or overdue when there is one, a dash
    /// (or the name, with `showsNotApplicable`) when there is no target,
    /// and the name of the state otherwise.
    static func visibleText(
        state: MonarchSlaIndicatorState,
        dueAt: Date?,
        now: Date,
        showsNotApplicable: Bool,
        labels: MonarchSlaIndicatorLabels,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        if state == .notApplicable, !showsNotApplicable {
            return labels.notApplicableMark ?? MonarchSlaIndicatorStrings.notApplicableMark
        }
        return timeText(state: state, dueAt: dueAt, now: now, labels: labels, locale: locale)
            ?? name(of: state, labels: labels)
    }

    /// Whether the due date is read: for every target that has one and is
    /// neither met nor absent.
    static func readsDueDate(state: MonarchSlaIndicatorState, dueAt: Date?) -> Bool {
        dueAt != nil && state != .met && state != .notApplicable
    }

    /// What VoiceOver reads as the label: the name of the target, then the
    /// time left or overdue in words, or the name of the state when there
    /// is no countdown.
    static func accessibilityLabel(
        state: MonarchSlaIndicatorState,
        dueAt: Date?,
        now: Date,
        target: String?,
        labels: MonarchSlaIndicatorLabels,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        let time = timeText(state: state, dueAt: dueAt, now: now, isSpoken: true, labels: labels, locale: locale)
        return [target, time ?? name(of: state, labels: labels)]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: spokenSeparator)
    }

    /// What VoiceOver reads as the value: the name of the state when the
    /// label is a countdown, the due date and time, and a note when an
    /// earlier target was breached. `dueText` is the formatted due date.
    static func accessibilityValue(
        state: MonarchSlaIndicatorState,
        dueAt: Date?,
        now: Date,
        dueText: String?,
        hasEarlierBreach: Bool,
        labels: MonarchSlaIndicatorLabels
    ) -> String {
        var parts: [String] = []
        if timeText(state: state, dueAt: dueAt, now: now, labels: labels) != nil {
            parts.append(name(of: state, labels: labels))
        }
        if readsDueDate(state: state, dueAt: dueAt), let dueText {
            parts.append(labels.due?(dueText) ?? MonarchSlaIndicatorStrings.due(dueText))
        }
        if hasEarlierBreach, state != .breached {
            parts.append(labels.earlierBreach ?? MonarchSlaIndicatorStrings.earlierBreach)
        }
        return parts.joined(separator: spokenSeparator)
    }

    /// The due date and time as text, such as "13 Nov 2026 at 15:00".
    static func dueText(_ dueAt: Date, locale: Locale = .autoupdatingCurrent, timeZone: TimeZone = .autoupdatingCurrent)
        -> String
    {
        var style = Date.FormatStyle(date: .abbreviated, time: .omitted).monarchTime()
        style.locale = locale
        style.timeZone = timeZone
        return dueAt.formatted(style)
    }

    /// What is announced when the state changes.
    static func changeAnnouncement(
        to state: MonarchSlaIndicatorState,
        target: String?,
        labels: MonarchSlaIndicatorLabels
    ) -> String {
        let stateName = name(of: state, labels: labels)
        return labels.changed?(stateName, target) ?? MonarchSlaIndicatorStrings.changed(stateName, target: target)
    }
}
