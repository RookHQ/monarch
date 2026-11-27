/// Built-in strings of the typing indicator, from the `MonarchTypingIndicator`
/// strings table.
enum MonarchTypingIndicatorStrings {
    private static let table = "MonarchTypingIndicator"

    /// One person is typing.
    static func one(_ name: String) -> String {
        MonarchStrings.string("monarch.typingIndicator.one", table: table, name)
    }

    /// Two people are typing.
    static func two(_ first: String, _ second: String) -> String {
        MonarchStrings.string("monarch.typingIndicator.two", table: table, first, second)
    }

    /// Three or more people are typing.
    static var several: String { MonarchStrings.string("monarch.typingIndicator.several", table: table) }
}
