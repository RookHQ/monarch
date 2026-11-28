/// Built-in strings of the context chips, from the `MonarchAiContextChips` strings table.
enum MonarchAiContextChipsStrings {
    private static let table = "MonarchAiContextChips"

    /// The spoken name of the list.
    static var label: String { MonarchStrings.string("monarch.aiContextChips.label", table: table) }

    /// The spoken prefix of a kind.
    static func kind(_ kind: MonarchAiContextKind) -> String {
        MonarchStrings.string("monarch.aiContextChips.kind.\(kind.rawValue)", table: table)
    }

    /// The total after the chips.
    static func total(_ formatted: String, count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.aiContextChips.total.one", table: table)
            : MonarchStrings.string("monarch.aiContextChips.total", table: table, formatted)
    }

    /// The spoken name of a remove button.
    static func remove(_ label: String) -> String {
        MonarchStrings.string("monarch.aiContextChips.remove", table: table, label)
    }

    /// Announced after an item is removed.
    static func removed(_ label: String) -> String {
        MonarchStrings.string("monarch.aiContextChips.removed", table: table, label)
    }
}
