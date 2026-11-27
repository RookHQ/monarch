import Foundation
import MonarchTokens
import SwiftUI

/// A part of ``MonarchBusinessHoursEditor`` whose problem shows once it has been changed.
enum MonarchBusinessHoursEditorField: Hashable {
    case name
    case hours
    case effective
    /// The holiday or special-hours row at an index.
    case exception(Int)
}

/// The messages the editor shows for a value: only the problems of the parts that have been
/// changed, or all of them once the app asks for them.
struct MonarchBusinessHoursEditorProblems: Equatable {
    var name: String?
    var open: String?
    var effective: String?
    var exceptions: [Int: String] = [:]

    /// Every message, in the order shown on the screen.
    var all: [String] {
        [name, effective, open].compactMap { $0 } + exceptions.keys.sorted().compactMap { exceptions[$0] }
    }
}

/// Component tokens and the pure editing logic of ``MonarchBusinessHoursEditor``.
enum MonarchBusinessHoursEditorTokens {
    /// The gap between a switch's title and its description.
    static let textGap: CGFloat = MonarchTokens.Space.space1
    /// The symbol of the add button.
    static let addSymbol = "plus"
    /// The symbol of the remove button and swipe action.
    static let removeSymbol = "minus.circle"
    /// The symbol of an effective day's clear button.
    static let clearSymbol = "xmark.circle"
    /// The role of notes and read-only values.
    static var muted: MonarchColorRole { \.textMuted }

    /// The longest name, as on the web.
    static let nameLimit = 100
    /// The first special hours of a date switched to open at other times: 10:00 to 16:00.
    static let specialStart = 600
    /// The end of the first special hours.
    static let specialEnd = 960

    /// UTC, the zone every date of the value is a day in.
    static let utc = TimeZone(identifier: "UTC") ?? TimeZone(secondsFromGMT: 0) ?? .current

    /// The Gregorian calendar in UTC, which compares and normalises the days of a value.
    static let dayCalendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = utc
        return calendar
    }()

    /// The calendar the date pickers run in: the environment's calendar and first weekday, in
    /// UTC, so the day chosen is the day stored whatever the device's zone.
    static func pickerCalendar(_ environment: Calendar) -> Calendar {
        var calendar = environment
        calendar.timeZone = utc
        return calendar
    }

    /// The start of a date's day in UTC.
    static func day(_ date: Date) -> Date {
        dayCalendar.startOfDay(for: date)
    }

    /// The person's today, in their zone, as the start of that day in UTC.
    static func today(now: Date, in timeZone: TimeZone) -> Date {
        var local = Calendar(identifier: .gregorian)
        local.timeZone = timeZone
        let parts = local.dateComponents([.year, .month, .day], from: now)
        return dayCalendar.date(from: parts) ?? day(now)
    }

    /// The date a "Choose a date" button picks: today, or the earliest allowed day if later.
    static func firstChoice(today: Date, minimum: Date?) -> Date {
        guard let minimum else { return today }
        return max(today, day(minimum))
    }

    /// A day as text in the read-only view and for VoiceOver, in UTC: "25 Dec 2026".
    static func dateText(_ date: Date, locale: Locale, calendar: Calendar) -> String {
        var style = Date.FormatStyle(
            date: .abbreviated,
            time: .omitted,
            locale: locale,
            calendar: pickerCalendar(calendar)
        )
        style.timeZone = utc
        return date.formatted(style)
    }

    /// A name cut to the longest name.
    static func limited(_ name: String) -> String {
        String(name.prefix(nameLimit))
    }

    /// The row switched to closed all day, or to open at other times from 10:00 to 16:00. Already
    /// open rows keep their hours.
    static func settingOpen(_ exception: MonarchBusinessHoursException, isOpen: Bool) -> MonarchBusinessHoursException {
        var next = exception
        if !isOpen {
            next.intervals = []
        } else if next.intervals.isEmpty {
            next.intervals = [MonarchBusinessHoursInterval(start: specialStart, end: specialEnd)]
        }
        return next
    }

    /// The row with its first interval's opening or closing time changed; other intervals stay.
    static func settingFirst(
        _ exception: MonarchBusinessHoursException,
        start: Int? = nil,
        end: Int? = nil
    ) -> MonarchBusinessHoursException {
        var next = exception
        guard var first = next.intervals.first else { return next }
        if let start { first.start = start }
        if let end { first.end = end }
        next.intervals[0] = first
        return next
    }

    /// The exceptions with one row replaced.
    static func replacing(
        _ exceptions: [MonarchBusinessHoursException],
        at index: Int,
        with exception: MonarchBusinessHoursException
    ) -> [MonarchBusinessHoursException] {
        guard exceptions.indices.contains(index) else { return exceptions }
        var next = exceptions
        next[index] = exception
        return next
    }

    /// The exceptions without one row.
    static func removing(_ exceptions: [MonarchBusinessHoursException], at index: Int)
        -> [MonarchBusinessHoursException]
    {
        exceptions.enumerated().filter { $0.offset != index }.map(\.element)
    }

    /// Where VoiceOver focus can be put in the list of dates.
    enum FocusTarget: Hashable {
        /// The heading of the date at this position, from 0.
        case date(Int)
        /// The Add date button, when no date is left.
        case add
    }

    /// Where focus goes after the date at `index` is removed from a list of
    /// `count` dates: the date now at that position, else the one before it,
    /// else the Add date button.
    static func focusAfterRemoval(removedIndex index: Int, count: Int) -> FocusTarget {
        MonarchFocusAfterRemoval.target(removedIndex: index, remaining: Array(0..<max(0, count - 1))).map(
            FocusTarget.date
        ) ?? .add
    }

    /// The changed parts after a row is removed: the row is forgotten and later rows move up.
    static func touched(_ touched: Set<MonarchBusinessHoursEditorField>, removing index: Int)
        -> Set<MonarchBusinessHoursEditorField>
    {
        Set(
            touched.compactMap { field in
                guard case .exception(let row) = field else { return field }
                if row == index { return nil }
                return .exception(row > index ? row - 1 : row)
            }
        )
    }

    /// The messages shown for a value's problems. The effective dates' order always shows,
    /// because it can only follow a choice; the others once their part is changed or when
    /// `showsErrors` is set.
    static func problems(
        _ errors: MonarchBusinessHoursErrors,
        touched: Set<MonarchBusinessHoursEditorField>,
        showsErrors: Bool,
        showsName: Bool,
        showsEffectiveDates: Bool,
        text: MonarchBusinessHoursEditorText
    ) -> MonarchBusinessHoursEditorProblems {
        func shows(_ field: MonarchBusinessHoursEditorField) -> Bool { showsErrors || touched.contains(field) }
        var out = MonarchBusinessHoursEditorProblems()
        if showsName, errors.isNameMissing, shows(.name) { out.name = text.nameRequired }
        if errors.isOpenIntervalMissing, shows(.hours) { out.open = text.noOpenHours }
        if showsEffectiveDates, errors.isEffectiveOrderInvalid { out.effective = text.effectiveOrder }
        for (index, error) in errors.exceptions where shows(.exception(index)) {
            out.exceptions[index] = text.exceptionError(error)
        }
        return out
    }

    /// The messages that appeared since the last value, to announce each once.
    static func newMessages(from old: [String], to new: [String]) -> [String] {
        new.filter { !old.contains($0) }
    }

    /// The read-only text of a row's hours: "Closed all day" or "10:00 AM – 4:00 PM".
    static func hoursText(
        _ exception: MonarchBusinessHoursException,
        text: MonarchBusinessHoursEditorText,
        weeklyText: MonarchWeeklyHoursEditorText,
        formats: MonarchWeeklyHoursEditorFormats
    ) -> String {
        guard !exception.intervals.isEmpty else { return text.closed }
        return exception.intervals.map { weeklyText.range(formats.time($0.start), formats.time($0.end)) }
            .formatted(.list(type: .and, width: .narrow).locale(formats.locale))
    }
}
