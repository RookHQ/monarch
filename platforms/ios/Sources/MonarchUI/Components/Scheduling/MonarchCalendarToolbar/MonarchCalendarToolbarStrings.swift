/// Built-in strings of the calendar toolbar, from the `MonarchCalendarToolbar` strings table.
enum MonarchCalendarToolbarStrings {
    private static let table = "MonarchCalendarToolbar"

    /// The Today button.
    static var today: String { MonarchStrings.string("monarch.calendarToolbar.today", table: table) }
    /// "Previous week".
    static func previous(_ unit: String) -> String {
        MonarchStrings.string("monarch.calendarToolbar.previous", table: table, unit)
    }
    /// "Next week".
    static func next(_ unit: String) -> String {
        MonarchStrings.string("monarch.calendarToolbar.next", table: table, unit)
    }
    /// The spoken name of the view switcher.
    static var view: String { MonarchStrings.string("monarch.calendarToolbar.view", table: table) }
    /// The spoken name of the busy indicator.
    static var loading: String { MonarchStrings.string("monarch.calendarToolbar.loading", table: table) }
    /// The name of a view.
    static func viewName(_ view: MonarchCalendarToolbarView) -> String {
        MonarchStrings.string("monarch.calendarToolbar.view.\(view.rawValue)", table: table)
    }
    /// The unit a view moves by.
    static func unit(_ view: MonarchCalendarToolbarView) -> String {
        MonarchStrings.string("monarch.calendarToolbar.unit.\(view.rawValue)", table: table)
    }
}
