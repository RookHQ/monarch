/// Built-in strings of the read receipt, from the `MonarchReadReceipt` strings table.
enum MonarchReadReceiptStrings {
    private static let table = "MonarchReadReceipt"

    /// The visible text of the text and avatars displays.
    static var seen: String { MonarchStrings.string("monarch.readReceipt.seen", table: table) }

    /// The visible text of the count display.
    static func seenByCount(_ count: Int) -> String {
        MonarchStrings.string("monarch.readReceipt.seenByCount", table: table, count)
    }

    /// The full sentence with the joined names.
    static func seenBy(_ names: String) -> String {
        MonarchStrings.string("monarch.readReceipt.seenBy", table: table, names)
    }
}
