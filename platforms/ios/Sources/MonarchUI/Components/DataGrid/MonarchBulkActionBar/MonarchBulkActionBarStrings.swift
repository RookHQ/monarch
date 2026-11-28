/// Built-in strings of ``MonarchBulkActionBar``, from the `MonarchBulkActionBar` strings table.
enum MonarchBulkActionBarStrings {
    private static let table = "MonarchBulkActionBar"

    /// "3 selected" or "3 of 120 selected".
    static func selected(_ count: Int, total: Int?) -> String {
        if let total {
            return MonarchStrings.string("monarch.bulkActionBar.selectedOfTotal", table: table, count, total)
        }
        return MonarchStrings.string("monarch.bulkActionBar.selected", table: table, count)
    }
    /// Announced when the selection is emptied.
    static var cleared: String { MonarchStrings.string("monarch.bulkActionBar.cleared", table: table) }
    /// The clear button.
    static var clear: String { MonarchStrings.string("monarch.bulkActionBar.clear", table: table) }
    /// The overflow menu button.
    static var more: String { MonarchStrings.string("monarch.bulkActionBar.more", table: table) }
    /// The spoken name of the bar.
    static var bar: String { MonarchStrings.string("monarch.bulkActionBar.bar", table: table) }
}
