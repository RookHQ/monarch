/// Replacements for the built-in strings of ``MonarchAiConversationList``.
/// Every value is optional; leave one out to keep the built-in English text.
public struct MonarchAiConversationListLabels: Sendable {
    /// The spoken name of the list.
    public var label: String
    /// The heading of a group.
    public var group: @Sendable (_ group: MonarchAiConversationGroup) -> String
    /// The title shown for a conversation without one.
    public var untitled: String
    /// The title of the new conversation button.
    public var newConversation: String
    /// The prompt of the search field.
    public var search: String
    /// The title shown when there are no conversations.
    public var empty: String
    /// The title shown when the search matches nothing.
    public var noResults: @Sendable (_ query: String) -> String
    /// Announced after the search changes: "3 conversations".
    public var results: @Sendable (_ count: Int) -> String
    /// The rename command.
    public var rename: String
    /// The pin command.
    public var pin: String
    /// The unpin command.
    public var unpin: String
    /// The delete command.
    public var delete: String
    /// The title of the rename dialog.
    public var renameTitle: String
    /// The name of the field in the rename dialog.
    public var renameField: String
    /// The save button of the rename dialog.
    public var save: String
    /// The cancel button of both dialogs.
    public var cancel: String
    /// The title of the delete confirmation, from the conversation's title.
    public var deleteTitle: @Sendable (_ title: String) -> String
    /// The message of the delete confirmation.
    public var deleteMessage: String
    /// The destructive button of the delete confirmation.
    public var deleteConfirm: String
    /// The button that shows more of a long history, with how many it adds.
    public var showMore: @Sendable (_ count: Int) -> String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - label: Replaces "Conversation history".
    ///   - group: Replaces the group headings.
    ///   - untitled: Replaces "New conversation" as a title.
    ///   - newConversation: Replaces the "New conversation" button title.
    ///   - search: Replaces "Search conversations".
    ///   - empty: Replaces "No conversations yet".
    ///   - noResults: Replaces "No conversations match “…”".
    ///   - results: Replaces "… conversations".
    ///   - rename: Replaces "Rename".
    ///   - pin: Replaces "Pin".
    ///   - unpin: Replaces "Unpin".
    ///   - delete: Replaces "Delete".
    ///   - renameTitle: Replaces "Rename conversation".
    ///   - renameField: Replaces "Conversation title".
    ///   - save: Replaces "Save".
    ///   - cancel: Replaces "Cancel".
    ///   - deleteTitle: Replaces "Delete “…”?".
    ///   - deleteMessage: Replaces the delete warning.
    ///   - deleteConfirm: Replaces "Delete conversation".
    ///   - showMore: Replaces "Show … more conversations".
    public init(
        label: String? = nil,
        group: (@Sendable (_ group: MonarchAiConversationGroup) -> String)? = nil,
        untitled: String? = nil,
        newConversation: String? = nil,
        search: String? = nil,
        empty: String? = nil,
        noResults: (@Sendable (_ query: String) -> String)? = nil,
        results: (@Sendable (_ count: Int) -> String)? = nil,
        rename: String? = nil,
        pin: String? = nil,
        unpin: String? = nil,
        delete: String? = nil,
        renameTitle: String? = nil,
        renameField: String? = nil,
        save: String? = nil,
        cancel: String? = nil,
        deleteTitle: (@Sendable (_ title: String) -> String)? = nil,
        deleteMessage: String? = nil,
        deleteConfirm: String? = nil,
        showMore: (@Sendable (_ count: Int) -> String)? = nil
    ) {
        typealias Strings = MonarchAiConversationListStrings
        self.label = label ?? Strings.label
        self.group = group ?? { Strings.group($0) }
        self.untitled = untitled ?? Strings.untitled
        self.newConversation = newConversation ?? Strings.newConversation
        self.search = search ?? Strings.search
        self.empty = empty ?? Strings.empty
        self.noResults = noResults ?? { Strings.noResults($0) }
        self.results = results ?? { Strings.results($0) }
        self.rename = rename ?? Strings.rename
        self.pin = pin ?? Strings.pin
        self.unpin = unpin ?? Strings.unpin
        self.delete = delete ?? Strings.delete
        self.renameTitle = renameTitle ?? Strings.renameTitle
        self.renameField = renameField ?? Strings.renameField
        self.save = save ?? Strings.save
        self.cancel = cancel ?? Strings.cancel
        self.deleteTitle = deleteTitle ?? { Strings.deleteTitle($0) }
        self.deleteMessage = deleteMessage ?? Strings.deleteMessage
        self.deleteConfirm = deleteConfirm ?? Strings.deleteConfirm
        self.showMore = showMore ?? { Strings.showMore($0) }
    }
}
