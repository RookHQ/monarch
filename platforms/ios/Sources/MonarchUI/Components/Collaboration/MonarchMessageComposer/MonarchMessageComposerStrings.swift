/// Built-in strings of the message composer, from the `MonarchMessageComposer` strings table.
enum MonarchMessageComposerStrings {
    private static let table = "MonarchMessageComposer"

    /// The send button.
    static var send: String { MonarchStrings.string("monarch.messageComposer.send", table: table) }
    /// The send button while editing.
    static var save: String { MonarchStrings.string("monarch.messageComposer.save", table: table) }
    /// The send button while sending.
    static var sending: String { MonarchStrings.string("monarch.messageComposer.sending", table: table) }
    /// The cancel button while editing.
    static var cancel: String { MonarchStrings.string("monarch.messageComposer.cancel", table: table) }
    /// The attach button.
    static var attach: String { MonarchStrings.string("monarch.messageComposer.attach", table: table) }
    /// The emoji button.
    static var emoji: String { MonarchStrings.string("monarch.messageComposer.emoji", table: table) }
    /// The name of the mode control.
    static var modes: String { MonarchStrings.string("monarch.messageComposer.modes", table: table) }
    /// The reply context.
    static func replyingTo(_ author: String) -> String {
        MonarchStrings.string("monarch.messageComposer.replyingTo", table: table, author)
    }
    /// The button that removes the reply context.
    static var dismissReply: String { MonarchStrings.string("monarch.messageComposer.dismissReply", table: table) }
    /// The edit context.
    static var editing: String { MonarchStrings.string("monarch.messageComposer.editing", table: table) }
    /// The spoken characters left, or over the limit when negative.
    static func remaining(_ count: Int) -> String {
        count >= 0
            ? MonarchStrings.string("monarch.messageComposer.remaining", table: table, count)
            : MonarchStrings.string("monarch.messageComposer.overLimit", table: table, -count)
    }
    /// The draft is being saved.
    static var draftSaving: String { MonarchStrings.string("monarch.messageComposer.draftSaving", table: table) }
    /// The draft was saved.
    static var draftSaved: String { MonarchStrings.string("monarch.messageComposer.draftSaved", table: table) }
    /// The draft could not be saved.
    static var draftError: String { MonarchStrings.string("monarch.messageComposer.draftError", table: table) }
    /// The line under the composer after a send failed.
    static var sendFailed: String { MonarchStrings.string("monarch.messageComposer.sendFailed", table: table) }

    /// The name of a formatting tool.
    static func format(_ tool: MonarchMessageComposerFormat) -> String {
        switch tool {
        case .bold: MonarchStrings.string("monarch.messageComposer.format.bold", table: table)
        case .italic: MonarchStrings.string("monarch.messageComposer.format.italic", table: table)
        case .strikethrough: MonarchStrings.string("monarch.messageComposer.format.strikethrough", table: table)
        case .code: MonarchStrings.string("monarch.messageComposer.format.code", table: table)
        case .bulletList: MonarchStrings.string("monarch.messageComposer.format.bulletList", table: table)
        case .numberedList: MonarchStrings.string("monarch.messageComposer.format.numberedList", table: table)
        case .link: MonarchStrings.string("monarch.messageComposer.format.link", table: table)
        case .quote: MonarchStrings.string("monarch.messageComposer.format.quote", table: table)
        case .codeBlock: MonarchStrings.string("monarch.messageComposer.format.codeBlock", table: table)
        }
    }
}
