/// One control of a ``MonarchEditorToolbar``.
public enum MonarchEditorTool: Sendable, Hashable {
    /// Undoes the last change. A plain button.
    case undo
    /// Redoes the last undone change. A plain button.
    case redo
    /// Body text. Grouped with the headings in the text style menu.
    case paragraph
    /// A heading of a level from 1 to 6. Grouped with body text in the text style menu.
    case heading(Int)
    /// Bold. A toggle.
    case bold
    /// Italic. A toggle.
    case italic
    /// Strikethrough. A toggle.
    case strikethrough
    /// Inline code. A toggle.
    case code
    /// A link. A toggle that opens the link form when the toolbar handles links.
    case link
    /// A bulleted list. A toggle.
    case bulletList
    /// A numbered list. A toggle.
    case orderedList
    /// A checklist. A toggle.
    case taskList
    /// A quote. A toggle.
    case blockquote
    /// A code block. A toggle.
    case codeBlock
    /// A product control, such as "Comment". A plain button.
    case custom(id: String, title: String, systemImage: String)

    /// Every built-in control in groups: history, text style, inline styles, lists, blocks.
    public static let defaultGroups: [[MonarchEditorTool]] = [
        [.undo, .redo],
        [.paragraph, .heading(1), .heading(2), .heading(3)],
        [.bold, .italic, .strikethrough, .code, .link],
        [.bulletList, .orderedList, .taskList],
        [.blockquote, .codeBlock],
    ]

    /// Inline formatting only, for a toolbar over a selection.
    public static let inlineGroups: [[MonarchEditorTool]] = [
        [.bold, .italic, .strikethrough, .code],
        [.link],
    ]
}
