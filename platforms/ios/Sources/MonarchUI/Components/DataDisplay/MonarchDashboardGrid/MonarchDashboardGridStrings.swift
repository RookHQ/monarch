/// Built-in strings of the dashboard grid, from the `MonarchDashboardGrid` strings table.
enum MonarchDashboardGridStrings {
    private static let table = "MonarchDashboardGrid"

    /// The spoken name of the grid.
    static var label: String { MonarchStrings.string("monarch.dashboardGrid.label", table: table) }
    /// The hint on an arrange bar.
    static var dragHint: String { MonarchStrings.string("monarch.dashboardGrid.dragHint", table: table) }

    /// "Move Team load earlier".
    static func moveEarlier(_ widget: String) -> String {
        MonarchStrings.string("monarch.dashboardGrid.moveEarlier", table: table, widget)
    }

    /// "Move Team load later".
    static func moveLater(_ widget: String) -> String {
        MonarchStrings.string("monarch.dashboardGrid.moveLater", table: table, widget)
    }

    /// "Team load moved to position 2 of 6."
    static func moved(_ widget: String, _ position: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.dashboardGrid.moved", table: table, widget, position, total)
    }

    /// "Team load dropped at position 2 of 6."
    static func dropped(_ widget: String, _ position: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.dashboardGrid.dropped", table: table, widget, position, total)
    }
}
