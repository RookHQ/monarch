/// Built-in strings of the shift roster, from the `MonarchShiftRoster` strings table.
enum MonarchShiftRosterStrings {
    private static let table = "MonarchShiftRoster"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.shiftRoster.\(key)", table: table)
    }

    /// "Shift roster".
    static var roster: String { string("roster") }
    /// "Person".
    static var person: String { string("person") }
    /// "Day".
    static var day: String { string("day") }
    /// "Off".
    static var off: String { string("off") }
    /// "Not assigned".
    static var unassigned: String { string("unassigned") }
    /// "Clear".
    static var clear: String { string("clear") }
    /// "overnight".
    static var overnight: String { string("overnight") }
    /// "Hours".
    static var hours: String { string("hours") }
    /// "Working".
    static var working: String { string("working") }
    /// "Today".
    static var today: String { string("today") }
    /// "No one to schedule".
    static var emptyTitle: String { string("emptyTitle") }
    /// "People appear here with their shift for each day."
    static var emptyDescription: String { string("emptyDescription") }
    /// "Loading roster".
    static var loading: String { string("loading") }
    /// "1 working".
    static var workingOne: String { string("workingOne") }

    /// "3 working".
    static func workingOther(_ count: String) -> String {
        MonarchStrings.string("monarch.shiftRoster.workingOther", table: table, count)
    }
    /// "Alex Kim, Wednesday, November 4".
    static func menuTitle(_ name: String, _ day: String) -> String {
        MonarchStrings.string("monarch.shiftRoster.menuTitle", table: table, name, day)
    }
    /// "Alex Kim, Wednesday, November 4: Morning, 6:00 AM to 2:00 PM".
    static func cell(_ name: String, _ day: String, _ assignment: String) -> String {
        MonarchStrings.string("monarch.shiftRoster.cell", table: table, name, day, assignment)
    }
    /// "6:00 AM to 2:00 PM".
    static func range(_ start: String, _ end: String) -> String {
        MonarchStrings.string("monarch.shiftRoster.range", table: table, start, end)
    }
    /// "37.5 h".
    static func hoursValue(_ hours: String) -> String {
        MonarchStrings.string("monarch.shiftRoster.hoursValue", table: table, hours)
    }
}
