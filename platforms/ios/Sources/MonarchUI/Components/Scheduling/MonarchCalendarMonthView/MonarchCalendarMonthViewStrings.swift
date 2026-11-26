/// Built-in strings of the month view, from the `MonarchCalendarMonthView` strings table.
enum MonarchCalendarMonthViewStrings {
    private static let table = "MonarchCalendarMonthView"

    /// Added to today's spoken name.
    static var today: String { MonarchStrings.string("monarch.calendarMonthView.today", table: table) }
    /// The count of one event.
    static var countOne: String { MonarchStrings.string("monarch.calendarMonthView.count.one", table: table) }

    /// The VoiceOver action that moves an event one day earlier.
    static var moveDayEarlier: String {
        MonarchStrings.string("monarch.calendarMonthView.moveDayEarlier", table: table)
    }
    /// The VoiceOver action that moves an event one day later.
    static var moveDayLater: String { MonarchStrings.string("monarch.calendarMonthView.moveDayLater", table: table) }
    /// The VoiceOver action that moves an event one week earlier.
    static var moveWeekEarlier: String {
        MonarchStrings.string("monarch.calendarMonthView.moveWeekEarlier", table: table)
    }
    /// The VoiceOver action that moves an event one week later.
    static var moveWeekLater: String { MonarchStrings.string("monarch.calendarMonthView.moveWeekLater", table: table) }

    /// The overflow button.
    static func more(_ count: String) -> String {
        MonarchStrings.string("monarch.calendarMonthView.more", table: table, count)
    }

    /// The count of several events, or none.
    static func countOther(_ count: String) -> String {
        MonarchStrings.string("monarch.calendarMonthView.count.other", table: table, count)
    }
}
