/// Built-in strings of the stat card, from the `MonarchStatCard` strings table.
enum MonarchStatCardStrings {
    private static let table = "MonarchStatCard"

    /// Read while the value loads.
    static var loading: String { MonarchStrings.string("monarch.statCard.loading", table: table) }
    /// The spoken name of the help button, given the stat's label.
    static func helpLabel(_ label: String) -> String {
        MonarchStrings.string("monarch.statCard.helpLabel", table: table, label)
    }
    /// The spoken hint of a stat that opens its details.
    static var drillDownHint: String { MonarchStrings.string("monarch.statCard.drillDownHint", table: table) }
}
