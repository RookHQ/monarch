import Foundation
import MonarchTokens
import SwiftUI

/// The address of one cell of a timesheet grid.
struct MonarchTimesheetGridCellKey: Hashable, Sendable {
    /// The row's identifier.
    let row: String
    /// The day's date.
    let day: Date
}

/// What the text typed in a cell reads as.
enum MonarchTimesheetGridEntry: Equatable, Sendable {
    /// A duration in minutes to save; 0 clears the cell.
    case save(Int)
    /// Text that cannot be read as a duration.
    case invalid
    /// A duration above the daily maximum.
    case tooLong
}

/// The colour roles of a cell of a timesheet grid.
struct MonarchTimesheetGridCellAppearance: Equatable {
    /// The cell's fill.
    let background: MonarchColorRole
    /// The border of an editable cell's field.
    let border: MonarchColorRole
    /// The value's text.
    let foreground: MonarchColorRole
}

/// Component tokens and pure logic for ``MonarchTimesheetGrid``.
enum MonarchTimesheetGridTokens {
    // MARK: Sizes

    /// The narrowest a day column is, before Dynamic Type scaling. Narrow enough that the first five days show beside
    /// the row names on a phone, as the web's do.
    static let dayColumnWidth: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The width of the first column, before Dynamic Type scaling; long names wrap inside it.
    static let rowHeaderWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space6
    /// The narrowest the total column is, before Dynamic Type scaling.
    static let totalColumnWidth: CGFloat = MonarchTokens.Space.space20
    /// The padding inside a grid cell.
    static let cellPadding: CGFloat = MonarchTokens.Space.space1
    /// The padding inside a heading or label cell.
    static let headerPadding: CGFloat = MonarchTokens.Space.space2
    /// The padding inside a cell's field.
    static let fieldPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The minimum height of a cell, the touch target.
    static let minCellHeight: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The width of the bar under today's title on a day page.
    static let todayBarWidth: CGFloat = MonarchTokens.Space.space6
    /// The height of the bar under today's heading.
    static let todayBarHeight: CGFloat = MonarchTokens.Space.space0_5
    /// The space between the grid and the message line, and between the
    /// sections of a day page.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The space between lines inside a heading or a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The space between an icon and its text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The space between the rows of a day page.
    static let pageRowSpacing: CGFloat = MonarchTokens.Space.space4
    /// The padding around the header of a day page.
    static let pagePadding: CGFloat = MonarchTokens.Space.space3

    // MARK: Symbols and text

    /// Marks a locked day.
    static let lockSymbol = "lock.fill"
    /// Marks a day above the daily maximum and an error.
    static let warningSymbol = "exclamationmark.circle.fill"
    /// The previous-day button.
    static let previousSymbol = "chevron.backward"
    /// The next-day button.
    static let nextSymbol = "chevron.forward"
    /// Shown, not read, in an empty cell.
    static let emptyMark = "\u{2013}"
    /// What separates the parts of spoken text.
    static let spokenSeparator = ", "
    /// What separates the parts of a line of text.
    static let lineSeparator = " · "
    /// The default daily maximum: 24 hours.
    static let defaultMaximum = 1440
    /// The seconds in a minute.
    static let secondsPerMinute = 60

    // MARK: Totals

    /// The total of a row over the days.
    static func rowTotal(_ row: MonarchTimesheetGridRow, days: [MonarchTimesheetGridDay]) -> Int {
        days.reduce(0) { $0 + row.minutes(on: $1.date) }
    }

    /// The total of a day over the rows.
    static func dayTotal(_ day: MonarchTimesheetGridDay, rows: [MonarchTimesheetGridRow]) -> Int {
        rows.reduce(0) { $0 + $1.minutes(on: day.date) }
    }

    /// The total of every row over every day.
    static func grandTotal(rows: [MonarchTimesheetGridRow], days: [MonarchTimesheetGridDay]) -> Int {
        rows.reduce(0) { $0 + rowTotal($1, days: days) }
    }

    /// Whether a day's total is above the daily maximum.
    static func isOverLimit(_ total: Int, maximum: Int) -> Bool {
        total > maximum
    }

    // MARK: State

    /// Whether a cell takes typing: the grid is editable, the row is not
    /// read-only and the day is not locked.
    static func isCellEditable(isEditable: Bool, row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay) -> Bool {
        isEditable && !row.isReadOnly && !day.isLocked
    }

    /// The reason read with the cells of a locked day, or `nil` when the
    /// day is not locked.
    static func lockText(_ day: MonarchTimesheetGridDay, labels: MonarchTimesheetGridLabels) -> String? {
        guard day.isLocked else { return nil }
        if let reason = day.lockReason, !reason.isEmpty { return reason }
        return MonarchTimesheetGridStrings.locked(labels)
    }

    /// Reads the text typed in a cell. Empty text clears the cell.
    static func read(
        _ text: String,
        maximum: Int,
        parse: (@Sendable (String) -> Int?)? = nil
    ) -> MonarchTimesheetGridEntry {
        let parsed: MonarchDurationMath.Parsed
        if let parse {
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            parsed = trimmed.isEmpty ? .empty : parse(trimmed).map { .minutes($0) } ?? .invalid
        } else {
            parsed = MonarchDurationMath.parse(text)
        }
        switch parsed {
        case .empty: return .save(0)
        case .invalid: return .invalid
        case .minutes(let minutes):
            if minutes < 0 { return .invalid }
            return minutes > maximum ? .tooLong : .save(minutes)
        }
    }

    /// The message for text that cannot be saved, or `nil` for a value to save.
    static func problem(
        _ entry: MonarchTimesheetGridEntry,
        maximum: Int,
        labels: MonarchTimesheetGridLabels
    ) -> String? {
        switch entry {
        case .save: nil
        case .invalid: MonarchTimesheetGridStrings.invalid(labels)
        case .tooLong: MonarchTimesheetGridStrings.tooLong(durationText(maximum, labels: labels), labels)
        }
    }

    /// The message for a failed save: the error's description, or the
    /// built-in message.
    static func failureMessage(for error: any Error, labels: MonarchTimesheetGridLabels) -> String {
        MonarchFailureMessage.message(for: error, fallback: MonarchTimesheetGridStrings.saveFailed(labels))
    }

    /// The cell reached from (`row`, `day`) by moving `rowStep` rows and
    /// `dayStep` days, skipping cells that do not take typing. `nil` when
    /// the edge is reached first.
    static func nextCell(
        row: Int,
        day: Int,
        rowStep: Int,
        dayStep: Int,
        rowCount: Int,
        dayCount: Int,
        isEditable: (Int, Int) -> Bool
    ) -> (row: Int, day: Int)? {
        guard rowStep != 0 || dayStep != 0 else { return nil }
        var nextRow = row + rowStep
        var nextDay = day + dayStep
        while nextRow >= 0, nextRow < rowCount, nextDay >= 0, nextDay < dayCount {
            if isEditable(nextRow, nextDay) { return (nextRow, nextDay) }
            nextRow += rowStep
            nextDay += dayStep
        }
        return nil
    }

    /// The page shown first: the selected day, else today, else the first day.
    static func dayIndex(selected: Date?, days: [MonarchTimesheetGridDay]) -> Int {
        if let selected, let index = days.firstIndex(where: { $0.date == selected }) { return index }
        return days.firstIndex(where: \.isToday) ?? 0
    }

    /// Whether the grid is used rather than day pages.
    static func usesGrid(
        _ layout: MonarchTimesheetGridLayout,
        isRegularWidth: Bool,
        isAccessibilitySize: Bool
    ) -> Bool {
        switch layout {
        case .grid: true
        case .day: false
        case .automatic: isRegularWidth && !isAccessibilitySize
        }
    }

    // MARK: Text

    /// Minutes as "1h 30m", or by the labels' formatter.
    static func durationText(_ minutes: Int, labels: MonarchTimesheetGridLabels) -> String {
        labels.duration?(minutes) ?? MonarchDurationMath.format(minutes)
    }

    /// The text of a cell or total: the duration, or a dash when there is
    /// no time.
    static func valueText(_ minutes: Int, labels: MonarchTimesheetGridLabels) -> String {
        minutes > 0 ? durationText(minutes, labels: labels) : emptyMark
    }

    /// Minutes in words, for VoiceOver: "1 hour, 30 minutes", or "No time".
    static func spokenDuration(
        _ minutes: Int,
        labels: MonarchTimesheetGridLabels,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        guard minutes > 0 else { return MonarchTimesheetGridStrings.noTime(labels) }
        let style = Duration.UnitsFormatStyle(allowedUnits: [.hours, .minutes], width: .wide, zeroValueUnits: .hide)
        return Duration.seconds(minutes * secondsPerMinute).formatted(style.locale(locale))
    }

    /// A day's heading, such as "Mon 12".
    static func shortDayText(
        _ day: MonarchTimesheetGridDay,
        labels: MonarchTimesheetGridLabels,
        locale: Locale = .autoupdatingCurrent,
        timeZone: TimeZone = .autoupdatingCurrent
    ) -> String {
        if let label = day.label { return label }
        if let custom = labels.shortDay { return custom(day.date) }
        var style = Date.FormatStyle().weekday(.abbreviated).day()
        style.locale = locale
        style.timeZone = timeZone
        return day.date.formatted(style)
    }

    /// A day in cell names and on a day page, such as "Mon, Jan 12".
    static func longDayText(
        _ day: MonarchTimesheetGridDay,
        labels: MonarchTimesheetGridLabels,
        locale: Locale = .autoupdatingCurrent,
        timeZone: TimeZone = .autoupdatingCurrent
    ) -> String {
        if let custom = labels.longDay { return custom(day.date) }
        var style = Date.FormatStyle().weekday(.abbreviated).day().month(.abbreviated)
        style.locale = locale
        style.timeZone = timeZone
        return day.date.formatted(style)
    }

    /// The supporting line of a row on a day page: its description and its
    /// total for the period, "Billable · Total 7h 30m".
    static func pageRowHelper(
        _ row: MonarchTimesheetGridRow,
        days: [MonarchTimesheetGridDay],
        labels: MonarchTimesheetGridLabels
    ) -> String {
        let total = MonarchTimesheetGridStrings.rowTotal(
            durationText(rowTotal(row, days: days), labels: labels),
            labels
        )
        return [row.description, total]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: lineSeparator)
    }

    /// The day total's line under it: "of 8h", or `nil` when no time is expected.
    static func expectedText(_ day: MonarchTimesheetGridDay, labels: MonarchTimesheetGridLabels) -> String? {
        guard let expected = day.expectedMinutes, expected > 0 else { return nil }
        return MonarchTimesheetGridStrings.expected(durationText(expected, labels: labels), labels)
    }

    // MARK: Accessibility

    /// The name of a cell: the row and the day.
    static func cellLabel(rowName: String, dayText: String, labels: MonarchTimesheetGridLabels) -> String {
        MonarchTimesheetGridStrings.cellLabel(rowName, dayText, labels)
    }

    /// What VoiceOver reads as a read-only cell's value: the time in words
    /// and, for a locked day, the reason.
    static func cellValue(
        _ minutes: Int,
        lockText: String?,
        labels: MonarchTimesheetGridLabels,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        [spokenDuration(minutes, labels: labels, locale: locale), lockText]
            .compactMap { $0 }
            .joined(separator: spokenSeparator)
    }

    /// The hint of an editable cell: the error, else "Saving", else nothing.
    static func cellHint(error: String?, isSaving: Bool, labels: MonarchTimesheetGridLabels) -> String {
        if let error, !error.isEmpty { return MonarchTimesheetGridStrings.error(error) }
        return isSaving ? MonarchTimesheetGridStrings.saving(labels) : ""
    }

    /// What VoiceOver reads for a day's heading: the day, "Today", the
    /// lock reason and the note.
    static func dayHeadingLabel(
        _ day: MonarchTimesheetGridDay,
        labels: MonarchTimesheetGridLabels,
        locale: Locale = .autoupdatingCurrent,
        timeZone: TimeZone = .autoupdatingCurrent
    ) -> String {
        [
            longDayText(day, labels: labels, locale: locale, timeZone: timeZone),
            day.isToday ? MonarchTimesheetGridStrings.today(labels) : nil,
            lockText(day, labels: labels),
            day.note,
        ]
        .compactMap { $0 }
        .filter { !$0.isEmpty }
        .joined(separator: spokenSeparator)
    }

    /// What VoiceOver reads as a day total's value: the time in words, the
    /// expected time and "Over the daily limit".
    static func dayTotalValue(
        _ day: MonarchTimesheetGridDay,
        total: Int,
        maximum: Int,
        labels: MonarchTimesheetGridLabels,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        [
            spokenDuration(total, labels: labels, locale: locale),
            expectedText(day, labels: labels),
            isOverLimit(total, maximum: maximum) ? MonarchTimesheetGridStrings.overLimit(labels) : nil,
        ]
        .compactMap { $0 }
        .joined(separator: spokenSeparator)
    }

    // MARK: Appearance

    /// The colour roles of a cell for its state. Locked and non-working
    /// days are shaded; an error is a danger border; text being saved is muted.
    static func cellAppearance(
        isEditable: Bool,
        isLocked: Bool,
        isNonWorking: Bool,
        hasError: Bool,
        isSaving: Bool
    ) -> MonarchTimesheetGridCellAppearance {
        let background: MonarchColorRole = isLocked || isNonWorking ? \.bgSubtle : \.bgSurface
        let border: MonarchColorRole =
            hasError ? \.dangerStrong : (isEditable ? \.borderInput : \.borderSubtle)
        let foreground: MonarchColorRole =
            isSaving || isLocked ? \.textMuted : \.textDefault
        return MonarchTimesheetGridCellAppearance(background: background, border: border, foreground: foreground)
    }

    /// The text role of a day's heading: today is in the accent, other days
    /// in the default text.
    static func headingForeground(isToday: Bool) -> MonarchColorRole {
        isToday ? \.accentStrong : \.textDefault
    }

    /// The text role of a day total: danger above the daily maximum.
    static func totalForeground(isOverLimit: Bool) -> MonarchColorRole {
        isOverLimit ? \.dangerStrong : \.textDefault
    }
}
