import Foundation

/// The weekday names and times of ``MonarchWeeklyHoursEditor`` in one locale. Times are
/// minutes from midnight and are formatted on a fixed UTC day, so a daylight-saving change
/// never shifts them.
struct MonarchWeeklyHoursEditorFormats {
    let locale: Locale
    /// `true` for a 24-hour clock, `false` for 12 hours, `nil` to follow the locale.
    let uses24HourClock: Bool?
    /// The end of the day on a 12-hour clock, "Midnight".
    let midnight: String

    /// Whether times read on a 24-hour clock: the setting, else the locale's preference.
    var is24Hour: Bool {
        uses24HourClock ?? Self.prefers24Hour(locale)
    }

    /// Whether a locale writes times on a 24-hour clock, from its hour symbol.
    static func prefers24Hour(_ locale: Locale) -> Bool {
        let pattern = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: locale) ?? ""
        return pattern.contains("H") || pattern.contains("k")
    }

    /// "Monday" for ISO weekday 1.
    func day(_ isoDay: Int) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = locale
        let symbols = calendar.standaloneWeekdaySymbols
        let index = MonarchWeeklyHoursEditorTokens.calendarWeekday(isoDay: min(max(isoDay, 1), 7)) - 1
        return symbols.indices.contains(index) ? symbols[index] : String(isoDay)
    }

    /// "9:00 AM" or "09:00" for minutes from midnight; the end of the day is "Midnight" or "24:00".
    func time(_ minutes: Int) -> String {
        if minutes >= MonarchWeeklyHoursEditorTokens.minutesPerDay {
            return is24Hour ? "24:00" : midnight
        }
        // The roster's formatter already formats minutes on a fixed UTC day.
        return MonarchShiftRosterFormats(
            locale: locale,
            calendar: Calendar(identifier: .gregorian),
            uses24HourClock: is24Hour
        )
        .time(max(minutes, 0))
    }
}
