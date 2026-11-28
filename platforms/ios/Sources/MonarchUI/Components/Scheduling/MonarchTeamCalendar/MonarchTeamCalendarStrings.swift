/// Built-in strings of the team calendar, from the `MonarchTeamCalendar` strings table.
enum MonarchTeamCalendarStrings {
    private static let table = "MonarchTeamCalendar"

    /// "Person".
    static var person: String { MonarchStrings.string("monarch.teamCalendar.person", table: table) }
    /// "Team calendar, November 2026".
    static func grid(_ range: String) -> String {
        MonarchStrings.string("monarch.teamCalendar.grid", table: table, range)
    }
    /// "Today".
    static var today: String { MonarchStrings.string("monarch.teamCalendar.today", table: table) }
    /// "Holiday: Thanksgiving".
    static func holiday(_ name: String) -> String {
        MonarchStrings.string("monarch.teamCalendar.holiday", table: table, name)
    }
    /// "Monday, November 2 to Friday, November 6".
    static func range(_ first: String, _ last: String) -> String {
        MonarchStrings.string("monarch.teamCalendar.range", table: table, first, last)
    }
    /// "first half".
    static var firstHalf: String { MonarchStrings.string("monarch.teamCalendar.firstHalf", table: table) }
    /// "second half".
    static var secondHalf: String { MonarchStrings.string("monarch.teamCalendar.secondHalf", table: table) }
    /// "pending".
    static var pending: String { MonarchStrings.string("monarch.teamCalendar.pending", table: table) }
    /// "Nobody to show".
    static var emptyTitle: String { MonarchStrings.string("monarch.teamCalendar.emptyTitle", table: table) }
    /// "People and their time away appear here."
    static var emptyMessage: String { MonarchStrings.string("monarch.teamCalendar.emptyMessage", table: table) }
    /// The line under the grid.
    static var legend: String { MonarchStrings.string("monarch.teamCalendar.legend", table: table) }
    /// "Loading team calendar".
    static var loading: String { MonarchStrings.string("monarch.teamCalendar.loading", table: table) }
}
