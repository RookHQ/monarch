/// Built-in strings of the file card, from the `MonarchFileCard` strings table.
enum MonarchFileCardStrings {
    private static let table = "MonarchFileCard"

    /// The spoken name of the selection checkbox.
    static func select(_ name: String) -> String {
        MonarchStrings.string("monarch.fileCard.select", table: table, name)
    }

    /// The spoken name of the actions menu.
    static func actions(_ name: String) -> String {
        MonarchStrings.string("monarch.fileCard.actions", table: table, name)
    }
}
