import Foundation

/// Built-in strings of the calendar heatmap, from the `MonarchCalendarHeatmap` strings table.
enum MonarchCalendarHeatmapStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.calendarHeatmap.\(key)", table: "MonarchCalendarHeatmap")
    }

    /// The table heading of the dates.
    static var date: String { string("date") }
    /// The table heading of the values.
    static var value: String { string("value") }
    /// The table heading of the notes.
    static var note: String { string("note") }
    /// Read after today's value.
    static var today: String { string("today") }
    /// One day: count.
    static var dayCountOne: String { string("dayCountOne") }
    /// Several days: count.
    static var dayCount: String { string("dayCount") }
    /// The summary: days, first, last, total.
    static var summary: String { string("summary") }
    /// The highest day: the day as read.
    static var highest: String { string("highest") }

    /// Fills a format's `%@` placeholders.
    static func fill(_ format: String, _ arguments: String...) -> String {
        String(format: format, arguments: arguments)
    }
}
