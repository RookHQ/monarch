import Foundation

/// The day, time and number formats of ``MonarchShiftRoster`` in one locale.
/// Days are formatted in the roster's calendar and time zone; shift times,
/// which are minutes from midnight, are formatted on a fixed UTC day so a
/// daylight-saving change never shifts them.
struct MonarchShiftRosterFormats {
    let locale: Locale
    let calendar: Calendar
    /// `true` for a 24-hour clock, `false` for 12 hours, `nil` to follow the locale.
    let uses24HourClock: Bool?

    private var dayStyle: Date.FormatStyle {
        Date.FormatStyle(
            date: .omitted,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: calendar.timeZone
        )
    }

    /// "Wednesday, November 4".
    func fullDay(_ day: Date) -> String {
        day.formatted(dayStyle.weekday(.wide).month(.wide).day())
    }

    /// "Wed 4", the title of a day tab.
    func shortDay(_ day: Date) -> String {
        day.formatted(dayStyle.weekday(.abbreviated).day())
    }

    /// "Wed".
    func weekday(_ day: Date) -> String {
        day.formatted(dayStyle.weekday(.abbreviated))
    }

    /// "Nov 4".
    func date(_ day: Date) -> String {
        day.formatted(dayStyle.month(.abbreviated).day())
    }

    /// "6:00 AM" or "06:00" for a time in minutes from midnight, on a fixed
    /// UTC day (`Internal/MonarchFormats`).
    func time(_ minutes: Int) -> String {
        MonarchFormats.timeOfDay(minutes, locale: locale, uses24HourClock: uses24HourClock)
    }

    /// "37.5" for a number of hours, rounded to one decimal.
    func hours(_ hours: Double) -> String {
        hours.formatted(.number.precision(.fractionLength(0...1)).locale(locale))
    }

    /// "3", a count in the locale's digits.
    func count(_ count: Int) -> String {
        count.formatted(.number.locale(locale))
    }
}
