import Foundation

/// The date and number formats of ``MonarchGanttTimeline`` in one locale and calendar.
struct MonarchGanttTimelineFormats: Sendable {
    let days: MonarchGanttTimelineDays
    let locale: Locale

    private var base: Date.FormatStyle {
        Date.FormatStyle(locale: locale, calendar: days.calendar, timeZone: days.calendar.timeZone)
    }

    /// "Jan 12, 2026".
    func date(_ day: Int) -> String {
        days.date(day).formatted(base.year().month(.abbreviated).day())
    }

    /// "Jan", or "Jan 2026" with the year.
    func month(_ day: Int, withYear: Bool) -> String {
        let style = withYear ? base.year().month(.abbreviated) : base.month(.abbreviated)
        return days.date(day).formatted(style)
    }

    /// "M 12": the narrow weekday and the day of the month.
    func weekdayDay(_ day: Int) -> String {
        days.date(day).formatted(base.weekday(.narrow).day())
    }

    /// "12".
    func dayNumber(_ day: Int) -> String {
        days.date(day).formatted(base.day())
    }

    /// "60%" for a fraction from 0 to 1.
    func percent(_ fraction: Double) -> String {
        fraction.formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }
}
