/// Built-in strings of the time grid, from the `MonarchTimeGrid` strings table.
enum MonarchTimeGridStrings {
    private static let table = "MonarchTimeGrid"

    /// The label of the all-day row.
    static var allDay: String { MonarchStrings.string("monarch.timeGrid.allDay", table: table) }
    /// Added to today's heading.
    static var today: String { MonarchStrings.string("monarch.timeGrid.today", table: table) }
    /// "Create event, Wednesday 4 November, 09:00".
    static func createEvent(_ when: String) -> String {
        MonarchStrings.string("monarch.timeGrid.createEvent", table: table, when)
    }
    /// "Opens Wednesday 4 November".
    static func openDay(_ day: String) -> String {
        MonarchStrings.string("monarch.timeGrid.openDay", table: table, day)
    }
    /// "Move earlier".
    static var moveEarlier: String { MonarchStrings.string("monarch.timeGrid.moveEarlier", table: table) }
    /// "Move later".
    static var moveLater: String { MonarchStrings.string("monarch.timeGrid.moveLater", table: table) }
    /// "Move to previous day".
    static var moveToPreviousDay: String {
        MonarchStrings.string("monarch.timeGrid.moveToPreviousDay", table: table)
    }
    /// "Move to next day".
    static var moveToNextDay: String { MonarchStrings.string("monarch.timeGrid.moveToNextDay", table: table) }
}
