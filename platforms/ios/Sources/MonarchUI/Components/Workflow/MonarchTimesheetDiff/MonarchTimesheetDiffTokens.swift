import Foundation
import MonarchTokens
import SwiftUI

/// The kind of a line of a timesheet diff.
enum MonarchTimesheetDiffKind: Sendable, Hashable, CaseIterable {
    case added
    case removed
    case changed
}

/// The colour roles of a line of a timesheet diff.
struct MonarchTimesheetDiffAppearance: Equatable {
    /// The fill of the line.
    let background: MonarchColorRole
    /// The bar at the leading edge.
    let bar: MonarchColorRole
    /// The kind's text and symbol.
    let kind: MonarchColorRole
}

/// Component tokens and pure logic for ``MonarchTimesheetDiff``.
enum MonarchTimesheetDiffTokens {
    /// The space between the heading, the lines and the totals.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The space between lines.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The space between the parts of a line.
    static let partSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The space between the kind and the change of time.
    static let headSpacing: CGFloat = MonarchTokens.Space.space2
    /// The space between a symbol and the kind.
    static let symbolSpacing: CGFloat = MonarchTokens.Space.space1
    /// The padding above and below a line's content.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The padding before and after a line's content.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// The width of the bar at the leading edge of a line, as on the web.
    static let barWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The corner radius of a line.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The minutes in an hour.
    static let minutesPerHour = 60
    /// The seconds in a minute.
    static let secondsPerMinute = 60
    /// The minus sign of a negative change.
    static let minusSign = "\u{2212}"
    /// The plus sign of a positive change.
    static let plusSign = "+"
    /// What separates the parts of an entry's line.
    static let lineSeparator = " · "
    /// What separates the parts of spoken text.
    static let spokenSeparator = ", "

    /// The shape of a line.
    static var shape: RoundedRectangle {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
    }

    // MARK: Comparing

    /// The fields that differ between two versions of an entry, in the
    /// order date, time, project, activity, task, billable, note. A missing
    /// value and an empty one differ, as on the web.
    static func changedFields(
        _ before: MonarchTimesheetDiffEntry,
        _ after: MonarchTimesheetDiffEntry
    ) -> [MonarchTimesheetDiffField] {
        var fields: [MonarchTimesheetDiffField] = []
        if before.date != after.date { fields.append(.date) }
        if before.minutes != after.minutes { fields.append(.minutes) }
        if before.project != after.project { fields.append(.project) }
        if before.activity != after.activity { fields.append(.activity) }
        if before.task != after.task { fields.append(.task) }
        if before.isBillable != after.isBillable { fields.append(.billable) }
        if before.note != after.note { fields.append(.note) }
        return fields
    }

    // MARK: Durations

    /// Minutes as "2h", "45m", "1h 30m" or "0m", as on the web.
    static func durationText(_ minutes: Int) -> String {
        let value = max(0, minutes)
        guard value > 0 else { return "0m" }
        let hours = value / minutesPerHour
        let rest = value % minutesPerHour
        return [hours > 0 ? "\(hours)h" : nil, rest > 0 ? "\(rest)m" : nil]
            .compactMap { $0 }
            .joined(separator: " ")
    }

    /// Minutes in words, for VoiceOver: "1 hour, 30 minutes".
    static func spokenDuration(_ minutes: Int, locale: Locale = .autoupdatingCurrent) -> String {
        let value = max(0, minutes)
        let style = Duration.UnitsFormatStyle(
            allowedUnits: value == 0 ? [.minutes] : [.hours, .minutes],
            width: .wide,
            zeroValueUnits: value == 0 ? .show(length: 1) : .hide
        )
        return Duration.seconds(value * secondsPerMinute).formatted(style.locale(locale))
    }

    /// A signed change of minutes: "+1h 30m", "−45m" or "0m".
    static func deltaText(_ minutes: Int, labels: MonarchTimesheetDiffLabels) -> String {
        if let delta = labels.delta { return delta(minutes) }
        if minutes == 0 { return durationText(0) }
        return (minutes > 0 ? plusSign : minusSign) + durationText(abs(minutes))
    }

    /// A signed change of minutes in words: "plus 1 hour, 30 minutes".
    static func spokenDelta(_ minutes: Int, locale: Locale = .autoupdatingCurrent) -> String {
        if minutes == 0 { return MonarchTimesheetDiffStrings.unchanged }
        let duration = spokenDuration(abs(minutes), locale: locale)
        return minutes > 0
            ? MonarchTimesheetDiffStrings.increase(duration) : MonarchTimesheetDiffStrings.decrease(duration)
    }

    // MARK: Text

    /// The day of an entry, such as "Mon, Jan 12".
    static func dateText(_ date: Date, locale: Locale = .autoupdatingCurrent, timeZone: TimeZone = .autoupdatingCurrent)
        -> String
    {
        // "Mon, 12 Jan": a comma after the weekday, as on the web.
        var style = Date.FormatStyle().weekday(.abbreviated).day().month(.abbreviated)
        style.locale = locale
        style.timeZone = timeZone
        let text = date.formatted(style)
        guard let space = text.firstIndex(of: " "), !text[..<space].contains(",") else { return text }
        return text[..<space] + "," + text[space...]
    }

    /// An entry's line: the day, the time, the project, the activity, the
    /// task and "non-billable". With `isSpoken` the time is in words.
    static func entryText(
        _ entry: MonarchTimesheetDiffEntry,
        labels: MonarchTimesheetDiffLabels,
        isSpoken: Bool = false,
        locale: Locale = .autoupdatingCurrent,
        timeZone: TimeZone = .autoupdatingCurrent
    ) -> String {
        if let custom = labels.entry { return custom(entry) }
        let project = entry.project.flatMap { $0.isEmpty ? nil : $0 } ?? MonarchTimesheetDiffStrings.noProject(labels)
        let parts: [String?] = [
            dateText(entry.date, locale: locale, timeZone: timeZone),
            isSpoken ? spokenDuration(entry.minutes, locale: locale) : durationText(entry.minutes),
            project,
            entry.activity,
            entry.task,
            entry.isBillable == false ? MonarchTimesheetDiffStrings.nonBillable(labels) : nil,
        ]
        return
            parts
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: isSpoken ? spokenSeparator : lineSeparator)
    }

    /// The kind of a line: "Added", "Removed" or "Changed time, project".
    static func kindText(
        _ kind: MonarchTimesheetDiffKind,
        fields: [MonarchTimesheetDiffField] = [],
        labels: MonarchTimesheetDiffLabels
    ) -> String {
        switch kind {
        case .added: MonarchTimesheetDiffStrings.added(labels)
        case .removed: MonarchTimesheetDiffStrings.removed(labels)
        case .changed:
            MonarchTimesheetDiffStrings.changed(fields.map { MonarchTimesheetDiffStrings.field($0, labels) }, labels)
        }
    }

    /// The totals line: "Total +1h 30m · billable +1h".
    static func totalsText(
        _ totals: MonarchTimesheetDiffTotals,
        showsBillable: Bool,
        labels: MonarchTimesheetDiffLabels
    ) -> String {
        MonarchTimesheetDiffStrings.totals(
            deltaText(totals.total, labels: labels),
            billable: showsBillable ? deltaText(totals.billable, labels: labels) : nil,
            labels
        )
    }

    // MARK: Appearance

    /// The colour roles of a kind: success for added, danger for removed,
    /// warning for changed.
    static func appearance(_ kind: MonarchTimesheetDiffKind) -> MonarchTimesheetDiffAppearance {
        switch kind {
        case .added:
            MonarchTimesheetDiffAppearance(background: \.successSubtle, bar: \.successBorder, kind: \.successStrong)
        case .removed:
            MonarchTimesheetDiffAppearance(background: \.dangerSubtle, bar: \.dangerBorder, kind: \.dangerStrong)
        case .changed:
            MonarchTimesheetDiffAppearance(background: \.warningSubtle, bar: \.warningBorder, kind: \.warningStrong)
        }
    }

    /// The SF Symbol of a kind, so the kind is not told by colour alone.
    static func symbol(_ kind: MonarchTimesheetDiffKind) -> String {
        switch kind {
        case .added: "plus.circle"
        case .removed: "minus.circle"
        case .changed: "pencil.circle"
        }
    }

    // MARK: Accessibility

    /// What VoiceOver reads for an added or removed entry: the kind, the
    /// change of time and the entry.
    static func entryAccessibilityLabel(
        _ entry: MonarchTimesheetDiffEntry,
        kind: MonarchTimesheetDiffKind,
        labels: MonarchTimesheetDiffLabels,
        locale: Locale = .autoupdatingCurrent,
        timeZone: TimeZone = .autoupdatingCurrent
    ) -> String {
        let minutes = kind == .removed ? -entry.minutes : entry.minutes
        return [
            kindText(kind, labels: labels),
            spokenDelta(minutes, locale: locale),
            entryText(entry, labels: labels, isSpoken: true, locale: locale, timeZone: timeZone),
        ]
        .joined(separator: spokenSeparator)
    }

    /// What VoiceOver reads for a changed entry: the kind with its fields,
    /// the change of time when there is one, and the old and new lines.
    static func changeAccessibilityLabel(
        _ change: MonarchTimesheetDiffChange,
        labels: MonarchTimesheetDiffLabels,
        locale: Locale = .autoupdatingCurrent,
        timeZone: TimeZone = .autoupdatingCurrent
    ) -> String {
        let delta = change.after.minutes - change.before.minutes
        let before = entryText(change.before, labels: labels, isSpoken: true, locale: locale, timeZone: timeZone)
        let after = entryText(change.after, labels: labels, isSpoken: true, locale: locale, timeZone: timeZone)
        return [
            kindText(.changed, fields: change.fields, labels: labels),
            delta == 0 ? nil : spokenDelta(delta, locale: locale),
            MonarchTimesheetDiffStrings.before(labels) + spokenSeparator + before,
            MonarchTimesheetDiffStrings.after(labels) + spokenSeparator + after,
        ]
        .compactMap { $0 }
        .joined(separator: spokenSeparator)
    }
}
