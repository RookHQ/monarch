/// Built-in strings of the slash menu, from the `MonarchSlashMenu` strings table.
enum MonarchSlashMenuStrings {
    private static let table = "MonarchSlashMenu"

    /// The spoken name of the list.
    static var label: String { MonarchStrings.string("monarch.slashMenu.label", table: table) }

    /// Shown when nothing matches what was typed.
    static func empty(_ typed: String) -> String {
        MonarchStrings.string("monarch.slashMenu.empty", table: table, typed)
    }

    /// Announced when the list changes.
    static func count(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.slashMenu.countOne", table: table)
            : MonarchStrings.string("monarch.slashMenu.countOther", table: table, count)
    }
}
