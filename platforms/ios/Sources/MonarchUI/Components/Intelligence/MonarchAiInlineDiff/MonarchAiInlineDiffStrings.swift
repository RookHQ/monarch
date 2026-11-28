/// Built-in strings of the inline diff, from the `MonarchAiInlineDiff` strings table.
enum MonarchAiInlineDiffStrings {
    private static let table = "MonarchAiInlineDiff"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiInlineDiff.\(key)", table: table)
    }

    private static func string(_ key: String, _ first: Int, _ second: Int) -> String {
        MonarchStrings.string("monarch.aiInlineDiff.\(key)", table: table, first, second)
    }

    /// The spoken name of the diff.
    static var label: String { string("label") }
    /// Read before removed text.
    static var deleted: String { string("deleted") }
    /// Read before added text.
    static var inserted: String { string("inserted") }
    /// "Change 2 of 5".
    static func position(_ index: Int, _ total: Int) -> String { string("position", index, total) }
    /// Shown when there are no changes.
    static var none: String { string("none") }
    /// The previous button.
    static var previous: String { string("previous") }
    /// The next button.
    static var next: String { string("next") }
    /// The visible accept title.
    static var acceptTitle: String { string("acceptTitle") }
    /// The visible reject title.
    static var rejectTitle: String { string("rejectTitle") }
    /// The spoken name of the accept button.
    static func accept(_ index: Int, _ total: Int) -> String { string("accept", index, total) }
    /// The spoken name of the reject button.
    static func reject(_ index: Int, _ total: Int) -> String { string("reject", index, total) }

    /// Announced after a decision.
    static func announce(_ decision: MonarchAiInlineDiffDecision, left: Int) -> String {
        let key: String
        switch (decision, left > 0) {
        case (.accept, true): key = "acceptedLeft"
        case (.accept, false): key = "acceptedNone"
        case (.reject, true): key = "rejectedLeft"
        case (.reject, false): key = "rejectedNone"
        }
        return left > 0
            ? MonarchStrings.string("monarch.aiInlineDiff.\(key)", table: table, left)
            : string(key)
    }
}
