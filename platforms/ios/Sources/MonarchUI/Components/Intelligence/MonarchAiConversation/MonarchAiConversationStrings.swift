/// Built-in strings of the conversation, from the `MonarchAiConversation` strings table.
enum MonarchAiConversationStrings {
    private static let table = "MonarchAiConversation"

    /// The message of the error raised when the transport's stream throws.
    static var connectionLost: String { MonarchStrings.string("monarch.aiConversation.connectionLost", table: table) }
    /// The error of a tool call whose client-side tool threw without a message.
    static var toolResultFailed: String {
        MonarchStrings.string("monarch.aiConversation.toolResultFailed", table: table)
    }
}
