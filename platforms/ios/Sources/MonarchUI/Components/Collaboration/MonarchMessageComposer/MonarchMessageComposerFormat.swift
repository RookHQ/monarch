import Foundation

/// A tool of the formatting row of ``MonarchMessageComposer``.
enum MonarchMessageComposerFormat: Hashable, Identifiable, CaseIterable {
    case bold, italic, strikethrough, code, bulletList, numberedList, link, quote, codeBlock

    var id: Self { self }

    /// The SF Symbol of the tool.
    var symbol: String {
        switch self {
        case .bold: "bold"
        case .italic: "italic"
        case .strikethrough: "strikethrough"
        case .code: "chevron.left.forwardslash.chevron.right"
        case .bulletList: "list.bullet"
        case .numberedList: "list.number"
        case .link: "link"
        case .quote: "text.quote"
        case .codeBlock: "curlybraces.square"
        }
    }

    /// The Markdown the tool writes.
    var marker: String {
        switch self {
        case .bold: "**"
        case .italic: "_"
        case .strikethrough: "~~"
        case .code: "`"
        case .bulletList: "- "
        case .numberedList: "1. "
        case .link: "[](https://)"
        case .quote: "> "
        case .codeBlock: "```"
        }
    }
}
