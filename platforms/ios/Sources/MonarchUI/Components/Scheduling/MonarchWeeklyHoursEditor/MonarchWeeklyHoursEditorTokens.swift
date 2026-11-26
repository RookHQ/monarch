import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and the pure editing logic of ``MonarchWeeklyHoursEditor``.
enum MonarchWeeklyHoursEditorTokens {
    /// The gap between the heading and the days.
    static let dayGap: CGFloat = MonarchTokens.Space.space3
    /// The minimum width of a day name, so the controls of the days line up.
    static let nameWidth: CGFloat = 100
    /// The padding inside a day of the card.
    static let dayPadding: CGFloat = MonarchTokens.Space.space3
    /// The corner radius of the card around the days.
    static let cardRadius: CGFloat = CGFloat(MonarchTokens.Radius.lg)
    /// The gap between the controls of one day.
    static let controlGap: CGFloat = MonarchTokens.Space.space2
    /// The gap inside the day's label and the read-only text.
    static let textGap: CGFloat = MonarchTokens.Space.space1
    /// The symbol of the add button.
    static let addSymbol = "plus"
    /// The symbol of the copy button.
    static let copySymbol = "doc.on.doc"
    /// The symbol of the remove button.
    static let removeSymbol = "xmark"
    /// The mark between the start and end menus. Decorative; VoiceOver reads the menus' names.
    static let separator = "–"
    /// The role of "Unavailable" and of the read-only hours.
    static var muted: MonarchColorRole { \.textMuted }
    /// The role of the line between days.
    static var divider: MonarchColorRole { \.borderSubtle }

    /// Minutes in a day: the latest end.
    static let minutesPerDay = 1440
    /// The default minutes between time options, as on the web.
    static let defaultStep = 15
    /// The default most intervals per day, as on the web.
    static let defaultMaxIntervals = 6
    /// The default start of a day switched on for the first time: 09:00.
    static let defaultStart = 540
    /// The default end of a day switched on for the first time: 17:00.
    static let defaultEnd = 1020

    private static let maxStep = 60
    private static let hour = 60

    /// The step between time options: 1 to 60 minutes; zero or a negative step falls back to 15.
    static func step(_ step: Int) -> Int {
        step >= 1 ? min(maxStep, step) : defaultStep
    }

    /// The ISO weekday (1 Monday … 7 Sunday) of a `Calendar` weekday (1 Sunday … 7 Saturday).
    static func isoDay(calendarWeekday: Int) -> Int {
        let weekday = min(max(calendarWeekday, 1), 7)
        return weekday == 1 ? 7 : weekday - 1
    }

    /// The `Calendar` weekday (1 Sunday … 7 Saturday) of an ISO weekday.
    static func calendarWeekday(isoDay: Int) -> Int {
        isoDay % 7 + 1
    }

    /// The seven ISO weekdays in the order shown, starting at a `Calendar` weekday
    /// (1 Sunday … 7 Saturday), such as `calendar.firstWeekday`.
    static func order(firstWeekday: Int) -> [Int] {
        let first = isoDay(calendarWeekday: firstWeekday)
        return (0..<7).map { (first - 1 + $0) % 7 + 1 }
    }

    /// The intervals of a day, in the order they were arranged.
    static func dayOf(_ value: [MonarchWeeklyHoursInterval], _ day: Int) -> [MonarchWeeklyHoursInterval] {
        value.filter { $0.day == day }
    }

    /// The value with the intervals of a day replaced, ordered by ISO weekday and, within a day,
    /// in the order arranged. Intervals with a day outside 1 to 7 are dropped.
    static func withDay(
        _ value: [MonarchWeeklyHoursInterval],
        day: Int,
        ranges: [MonarchWeeklyHoursInterval]
    ) -> [MonarchWeeklyHoursInterval] {
        (1...7).flatMap { current in
            current == day
                ? ranges.map { MonarchWeeklyHoursInterval(day: current, start: $0.start, end: $0.end) }
                : dayOf(value, current)
        }
    }

    /// Every day gets the intervals of one day.
    static func copiedToAll(_ value: [MonarchWeeklyHoursInterval], from day: Int) -> [MonarchWeeklyHoursInterval] {
        let ranges = dayOf(value, day)
        return (1...7).flatMap { current in
            ranges.map { MonarchWeeklyHoursInterval(day: current, start: $0.start, end: $0.end) }
        }
    }

    /// The intervals a day gets when it is switched on: the ones it had when it was switched
    /// off, else one interval from the default start to the default end.
    static func switchedOn(
        day: Int,
        remembered: [MonarchWeeklyHoursInterval]?,
        defaultStart: Int,
        defaultEnd: Int
    ) -> [MonarchWeeklyHoursInterval] {
        if let remembered, !remembered.isEmpty { return remembered }
        return [MonarchWeeklyHoursInterval(day: day, start: defaultStart, end: defaultEnd)]
    }

    /// The interval the add button appends: an hour after the last one and an hour long, within
    /// the day. With no intervals, the default 09:00 to 17:00.
    static func nextInterval(after ranges: [MonarchWeeklyHoursInterval], day: Int, step: Int)
        -> MonarchWeeklyHoursInterval
    {
        guard let last = ranges.max(by: { $0.start < $1.start }) else {
            return MonarchWeeklyHoursInterval(day: day, start: defaultStart, end: defaultEnd)
        }
        let start = max(0, min(last.end + hour, minutesPerDay - step))
        return MonarchWeeklyHoursInterval(day: day, start: start, end: min(start + hour, minutesPerDay))
    }

    /// Whether the add button shows: below the most intervals per day.
    static func canAdd(count: Int, maxIntervals: Int) -> Bool {
        count < maxIntervals
    }

    /// The times a start can take: every step from midnight to a step before the end of the
    /// day, and the current time when it is off the step.
    static func startOptions(step: Int, current: Int) -> [Int] {
        options(from: 0, to: minutesPerDay - step, step: step, current: current)
    }

    /// The times an end can take: every step from the first step to the end of the day, and
    /// the current time when it is off the step.
    static func endOptions(step: Int, current: Int) -> [Int] {
        options(from: step, to: minutesPerDay, step: step, current: current)
    }

    private static func options(from lower: Int, to upper: Int, step: Int, current: Int) -> [Int] {
        let list = stride(from: 0, through: minutesPerDay, by: step).filter { $0 >= lower && $0 <= upper }
        return list.contains(current) ? list : (list + [current]).sorted()
    }

    /// The days whose problem is new or different, to announce them once when they appear.
    static func newErrors(
        from old: [Int: MonarchWeeklyHoursError],
        to new: [Int: MonarchWeeklyHoursError]
    ) -> [Int] {
        new.keys.filter { old[$0] != new[$0] }.sorted()
    }
}
