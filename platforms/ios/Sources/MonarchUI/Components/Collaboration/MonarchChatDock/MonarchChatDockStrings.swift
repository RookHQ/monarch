/// Built-in strings of the chat dock, from the `MonarchChatDock` strings table.
enum MonarchChatDockStrings {
    /// The name of the launcher and the chat.
    static var label: String { MonarchStrings.string("monarch.chatDock.label", table: "MonarchChatDock") }
    /// The unread count, with a `%d` for the number.
    static var unread: String { MonarchStrings.string("monarch.chatDock.unread", table: "MonarchChatDock") }
}
