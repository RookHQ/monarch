/// Built-in strings of the date range picker, from the `MonarchDateRangePicker` strings table.
enum MonarchDateRangePickerStrings {
    private static let table = "MonarchDateRangePicker"

    /// "Start".
    static var start: String { MonarchStrings.string("monarch.dateRangePicker.start", table: table) }
    /// "End".
    static var end: String { MonarchStrings.string("monarch.dateRangePicker.end", table: table) }
    /// "Choose dates".
    static var chooseDates: String { MonarchStrings.string("monarch.dateRangePicker.chooseDates", table: table) }
    /// "Clear dates".
    static var clear: String { MonarchStrings.string("monarch.dateRangePicker.clear", table: table) }
    /// "Done".
    static var done: String { MonarchStrings.string("monarch.dateRangePicker.done", table: table) }
    /// "Quick picks".
    static var presets: String { MonarchStrings.string("monarch.dateRangePicker.presets", table: table) }
    /// "That date is not available."
    static var unavailable: String { MonarchStrings.string("monarch.dateRangePicker.unavailable", table: table) }
    /// "1 day".
    static var dayCountOne: String { MonarchStrings.string("monarch.dateRangePicker.dayCountOne", table: table) }
    /// "7 days".
    static func dayCountOther(_ count: String) -> String {
        MonarchStrings.string("monarch.dateRangePicker.dayCountOther", table: table, count)
    }
}
