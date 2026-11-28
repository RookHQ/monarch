import Foundation

/// Calendar-day maths of ``MonarchGanttTimeline``. A day is a whole number:
/// the days since the start of 1 January 1970 in `calendar` (and its time
/// zone), so dates at any time of day map to their calendar day.
struct MonarchGanttTimelineDays: Sendable {
    /// The calendar, with the time zone, the days are read in.
    let calendar: Calendar
    private let origin: Date

    init(calendar: Calendar) {
        self.calendar = calendar
        origin = calendar.startOfDay(for: Date(timeIntervalSince1970: 0))
    }

    /// The day of a date.
    func day(_ date: Date) -> Int {
        calendar.dateComponents([.day], from: origin, to: calendar.startOfDay(for: date)).day ?? 0
    }

    /// The start of a day.
    func date(_ day: Int) -> Date {
        calendar.date(byAdding: .day, value: day, to: origin) ?? origin
    }

    /// The first day of the week (the calendar's `firstWeekday`) on or before `day`.
    func startOfWeek(_ day: Int) -> Int {
        let weekday = calendar.component(.weekday, from: date(day))
        let daysInWeek = calendar.weekdaySymbols.count
        return day - (weekday - calendar.firstWeekday + daysInWeek) % daysInWeek
    }

    /// Whether `day` starts a week.
    func isStartOfWeek(_ day: Int) -> Bool {
        calendar.component(.weekday, from: date(day)) == calendar.firstWeekday
    }

    /// Whether `day` is a weekend day in the calendar.
    func isWeekend(_ day: Int) -> Bool {
        calendar.isDateInWeekend(date(day))
    }

    /// The day of the month (1…31) and the month (1…12) of `day`.
    func dayAndMonth(_ day: Int) -> (day: Int, month: Int) {
        let parts = calendar.dateComponents([.day, .month], from: date(day))
        return (parts.day ?? 1, parts.month ?? 1)
    }
}
