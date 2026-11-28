/// Built-in strings of the mention picker, from the `MonarchMentionPicker` strings table.
enum MonarchMentionPickerStrings {
    private static let table = "MonarchMentionPicker"

    /// The spoken name of the list.
    static var label: String { MonarchStrings.string("monarch.mentionPicker.label", table: table) }
    /// The heading of people.
    static var people: String { MonarchStrings.string("monarch.mentionPicker.people", table: table) }
    /// The heading of channels.
    static var channels: String { MonarchStrings.string("monarch.mentionPicker.channels", table: table) }
    /// Read while searching.
    static var loading: String { MonarchStrings.string("monarch.mentionPicker.loading", table: table) }
    /// Shown when nothing matches.
    static var empty: String { MonarchStrings.string("monarch.mentionPicker.empty", table: table) }
    /// Shown when the search failed.
    static var error: String { MonarchStrings.string("monarch.mentionPicker.error", table: table) }

    /// Announced when results arrive.
    static func count(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.mentionPicker.countOne", table: table)
            : MonarchStrings.string("monarch.mentionPicker.countOther", table: table, count)
    }
}
