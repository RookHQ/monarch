/// Built-in strings of the time zone picker, from the `MonarchTimezoneSelect` strings table.
enum MonarchTimezoneSelectStrings {
    private static let table = "MonarchTimezoneSelect"

    static var detected: String { MonarchStrings.string("monarch.timezoneSelect.detected", table: table) }
    static var pinned: String { MonarchStrings.string("monarch.timezoneSelect.pinned", table: table) }
    static var recent: String { MonarchStrings.string("monarch.timezoneSelect.recent", table: table) }
    static var all: String { MonarchStrings.string("monarch.timezoneSelect.all", table: table) }
    static var empty: String { MonarchStrings.string("monarch.timezoneSelect.empty", table: table) }

    /// The default option label. The first argument is the offset, the second the city.
    static func label(_ offset: String, _ city: String) -> String {
        MonarchStrings.string("monarch.timezoneSelect.label", table: table, offset, city)
    }
}
