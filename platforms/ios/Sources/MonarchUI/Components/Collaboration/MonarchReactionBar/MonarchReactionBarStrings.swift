/// Built-in strings of the reaction bar, from the `MonarchReactionBar` strings table.
enum MonarchReactionBarStrings {
    private static let table = "MonarchReactionBar"

    /// The spoken name of the bar.
    static var label: String { MonarchStrings.string("monarch.reactionBar.label", table: table) }

    /// The spoken name of a chip.
    static func reaction(_ name: String, _ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.reactionBar.reactionOne", table: table, name)
            : MonarchStrings.string("monarch.reactionBar.reactionOther", table: table, name, count)
    }

    /// The add button.
    static var add: String { MonarchStrings.string("monarch.reactionBar.add", table: table) }

    /// The heading of who reacted.
    static var reactedBy: String { MonarchStrings.string("monarch.reactionBar.reactedBy", table: table) }
}
