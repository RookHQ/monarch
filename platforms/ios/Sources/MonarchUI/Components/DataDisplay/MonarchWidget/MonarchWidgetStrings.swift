/// Built-in strings of ``MonarchWidget``, from the `MonarchWidget` strings table.
enum MonarchWidgetStrings {
    private static let table = "MonarchWidget"

    /// The spoken name of the menu button.
    static func menu(_ title: String) -> String {
        MonarchStrings.string("monarch.widget.menu", table: table, title)
    }
    /// The visible title of the view-all button.
    static var viewAll: String { MonarchStrings.string("monarch.widget.viewAll", table: table) }
    /// The spoken name of the view-all button.
    static func viewAllLabel(_ title: String) -> String {
        MonarchStrings.string("monarch.widget.viewAllLabel", table: table, title)
    }
    /// The spoken name of the loading placeholder.
    static func loading(_ title: String) -> String {
        MonarchStrings.string("monarch.widget.loading", table: table, title)
    }
    /// The failure message.
    static var error: String { MonarchStrings.string("monarch.widget.error", table: table) }
    /// The empty title.
    static var empty: String { MonarchStrings.string("monarch.widget.empty", table: table) }
    /// The last-updated line.
    static func updated(_ relative: String) -> String {
        MonarchStrings.string("monarch.widget.updated", table: table, relative)
    }
}
