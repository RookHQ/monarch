/// Built-in strings of the sortable grid, from the `MonarchSortableGrid` strings table.
enum MonarchSortableGridStrings {
    private static let table = "MonarchSortableGrid"

    /// The spoken name of a tile's drag handle.
    static func handle(_ label: String) -> String {
        MonarchStrings.string("monarch.sortableGrid.handle", table: table, label)
    }

    /// Announced after a move with an accessibility action.
    static func moved(_ label: String, _ position: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.sortableGrid.moved", table: table, label, position, total)
    }

    /// Announced after a drop.
    static func dropped(_ label: String, _ position: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.sortableGrid.dropped", table: table, label, position, total)
    }
}
