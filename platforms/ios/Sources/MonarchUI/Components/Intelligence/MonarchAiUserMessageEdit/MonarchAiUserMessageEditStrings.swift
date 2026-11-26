/// Built-in strings of the message editor, from the `MonarchAiUserMessageEdit` strings table.
enum MonarchAiUserMessageEditStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiUserMessageEdit." + key, table: "MonarchAiUserMessageEdit")
    }

    /// The spoken label of the field.
    static var label: String { string("label") }
    /// The note that resending keeps the original.
    static var branchHint: String { string("branchHint") }
    /// The title of the button that resends.
    static var send: String { string("send") }
    /// The title of the button that leaves edit mode.
    static var cancel: String { string("cancel") }
}
