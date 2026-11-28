/// Built-in strings of the conversation list, from the `MonarchAiConversationList` strings table.
enum MonarchAiConversationListStrings {
    private static let table = "MonarchAiConversationList"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiConversationList." + key, table: table)
    }

    private static func format(_ key: String, _ argument: any CVarArg) -> String {
        MonarchStrings.string("monarch.aiConversationList." + key, table: table, argument)
    }

    /// The spoken name of the list.
    static var label: String { string("label") }
    /// The heading of a group.
    static func group(_ group: MonarchAiConversationGroup) -> String { string("group." + group.rawValue) }
    /// The title of an untitled conversation.
    static var untitled: String { string("untitled") }
    /// The new conversation button.
    static var newConversation: String { string("newConversation") }
    /// The search prompt.
    static var search: String { string("search") }
    /// The empty title.
    static var empty: String { string("empty") }
    /// The no results title.
    static func noResults(_ query: String) -> String { format("noResults", query) }
    /// The results announcement.
    static func results(_ count: Int) -> String {
        count == 1 ? string("results.one") : format("results.other", count)
    }
    /// The rename command.
    static var rename: String { string("rename") }
    /// The pin command.
    static var pin: String { string("pin") }
    /// The unpin command.
    static var unpin: String { string("unpin") }
    /// The delete command.
    static var delete: String { string("delete") }
    /// The rename dialog title.
    static var renameTitle: String { string("renameTitle") }
    /// The rename field name.
    static var renameField: String { string("renameField") }
    /// The save button.
    static var save: String { string("save") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// The delete confirmation title.
    static func deleteTitle(_ title: String) -> String { format("deleteTitle", title) }
    /// The delete confirmation message.
    static var deleteMessage: String { string("deleteMessage") }
    /// The delete confirmation button.
    static var deleteConfirm: String { string("deleteConfirm") }
    /// The show more button.
    static func showMore(_ count: Int) -> String {
        count == 1 ? string("showMore.one") : format("showMore.other", count)
    }
}
