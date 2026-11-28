/// Built-in strings of the message version switcher, from the `MonarchAiMessageBranch` strings table.
enum MonarchAiMessageBranchStrings {
    private static let table = "MonarchAiMessageBranch"

    /// The spoken name of the switcher.
    static var label: String { MonarchStrings.string("monarch.aiMessageBranch.label", table: table) }
    /// The spoken name of the previous button.
    static var previous: String { MonarchStrings.string("monarch.aiMessageBranch.previous", table: table) }
    /// The spoken name of the next button.
    static var next: String { MonarchStrings.string("monarch.aiMessageBranch.next", table: table) }

    /// The visible position, 1-based: "2 / 3".
    static func position(_ index: Int, of count: Int) -> String {
        MonarchStrings.string("monarch.aiMessageBranch.position", table: table, index, count)
    }

    /// The spoken position, 1-based: "Version 2 of 3".
    static func announcement(_ index: Int, of count: Int) -> String {
        MonarchStrings.string("monarch.aiMessageBranch.announcement", table: table, index, count)
    }
}
