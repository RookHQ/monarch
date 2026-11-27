import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchMiniMonth``.
enum MonarchMiniMonthTokens {
    /// The largest number of dots under one day.
    static let maximumDots = 3
    /// The diameter of a dot.
    static let dotSize: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between two dots.
    static let dotSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The colour role of the selected day and of today's number.
    static var tint: MonarchColorRole { \.accentSolid }

    /// The colour role of a dot. A dot with no tone is the accent.
    static func dotRole(_ tone: MonarchTone?) -> MonarchColorRole {
        switch tone {
        case nil: \.accentSolid
        case .neutral: \.textSubtle
        case .info: \.infoSolid
        case .success: \.successSolid
        case .warning: \.warningSolid
        case .danger: \.dangerSolid
        case .accent: \.accentSolid
        case .ai: \.aiSolid
        }
    }

    /// The colour roles of the dots of a day: at most three, in order.
    static func dotRoles(_ info: MonarchMiniMonthDayInfo?) -> [MonarchColorRole] {
        guard let info else { return [] }
        return info.tones.prefix(maximumDots).map(dotRole)
    }

    /// The first moment of the day a date falls on.
    static func day(_ date: Date, calendar: Calendar) -> Date {
        calendar.startOfDay(for: date)
    }

    /// Whether two dates fall on the same day.
    static func isSameDay(_ first: Date?, _ second: Date?, calendar: Calendar) -> Bool {
        switch (first, second) {
        case (nil, nil): true
        case (let first?, let second?): calendar.isDate(first, inSameDayAs: second)
        default: false
        }
    }

    /// Whether two dates fall in the same month of the same year.
    static func isSameMonth(_ first: Date, _ second: Date, calendar: Calendar) -> Bool {
        calendar.isDate(first, equalTo: second, toGranularity: .month)
    }

    /// The day of a date as the components the system calendar view takes.
    static func dayComponents(_ date: Date, calendar: Calendar) -> DateComponents {
        calendar.dateComponents([.calendar, .era, .year, .month, .day], from: date)
    }

    /// The month of a date as the components the system calendar view takes.
    static func monthComponents(_ date: Date, calendar: Calendar) -> DateComponents {
        calendar.dateComponents([.calendar, .era, .year, .month], from: date)
    }

    /// The dates that can be shown and chosen: from the first moment of the
    /// earliest day to the last moment of the latest day. A missing bound is
    /// the distant past or future. A latest day before the earliest day
    /// leaves only the earliest day, so the range is never invalid.
    static func availableRange(minimumDate: Date?, maximumDate: Date?, calendar: Calendar) -> DateInterval {
        let start = minimumDate.map { day($0, calendar: calendar) } ?? .distantPast
        guard let maximumDate else {
            return DateInterval(start: start, end: max(start, .distantFuture))
        }
        let lastDay = max(day(maximumDate, calendar: calendar), start)
        let nextDay = calendar.date(byAdding: .day, value: 1, to: lastDay) ?? lastDay
        // One second before the next day, so the latest day is included and
        // the day after is not.
        return DateInterval(start: start, end: max(start, nextDay.addingTimeInterval(-1)))
    }

    /// Whether a day is inside the earliest and latest days.
    static func isWithinBounds(_ date: Date, minimumDate: Date?, maximumDate: Date?, calendar: Calendar) -> Bool {
        availableRange(minimumDate: minimumDate, maximumDate: maximumDate, calendar: calendar)
            .contains(day(date, calendar: calendar))
    }

    /// A date moved into the earliest and latest days, for the visible month.
    static func clamped(_ date: Date, minimumDate: Date?, maximumDate: Date?, calendar: Calendar) -> Date {
        let range = availableRange(minimumDate: minimumDate, maximumDate: maximumDate, calendar: calendar)
        return min(max(date, range.start), range.end)
    }

    /// Whether a day can be chosen: inside the bounds and not disabled by
    /// the app.
    @MainActor
    static func isSelectable(
        _ date: Date,
        minimumDate: Date?,
        maximumDate: Date?,
        isDateDisabled: (@MainActor (Date) -> Bool)?,
        calendar: Calendar
    ) -> Bool {
        guard isWithinBounds(date, minimumDate: minimumDate, maximumDate: maximumDate, calendar: calendar) else {
            return false
        }
        return !(isDateDisabled?(day(date, calendar: calendar)) ?? false)
    }

    /// Every day of the month a date falls in, for reloading the dots.
    static func daysOfMonth(containing date: Date, calendar: Calendar) -> [Date] {
        guard let month = calendar.dateInterval(of: .month, for: date) else { return [] }
        var days: [Date] = []
        var current = month.start
        while current < month.end {
            days.append(current)
            guard let next = calendar.date(byAdding: .day, value: 1, to: current), next > current else { break }
            current = next
        }
        return days
    }
}
