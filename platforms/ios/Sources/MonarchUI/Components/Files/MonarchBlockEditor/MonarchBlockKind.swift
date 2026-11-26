/// What a block of a ``MonarchBlockEditor`` document is.
@_spi(Experimental) public enum MonarchBlockKind: Sendable, Hashable {
    /// Body text.
    case paragraph
    /// A heading of level 1 to 6. Levels outside are read as the nearest one.
    case heading(Int)
    /// An item of a bulleted list.
    case bulletList
    /// An item of a numbered list. Consecutive items are numbered together.
    case orderedList
    /// An item of a checklist, with a checkbox for ``MonarchBlock/isChecked``.
    case taskList
    /// A quotation.
    case blockquote
    /// Monospaced code. Its text keeps line breaks.
    case codeBlock
    /// A highlighted note in a tone, drawn with ``MonarchCalloutBlock``.
    case callout(MonarchTone)
    /// A collapsible section, drawn with ``MonarchToggleBlock``: the text is
    /// the summary and ``MonarchBlock/detail`` the content.
    case toggle
    /// A table, drawn with ``MonarchTableBlock`` from ``MonarchBlock/table``.
    case table
    /// A framed video, design or demo, drawn with ``MonarchEmbedBlock``: the
    /// text is the link and ``MonarchBlock/detail`` the caption.
    case embed

    /// Every kind the editor offers by default, in menu order: text, headings
    /// 1 to 3, lists, quote, code, an info callout, toggle, table and embed.
    public static let defaultKinds: [MonarchBlockKind] = [
        .paragraph, .heading(1), .heading(2), .heading(3),
        .bulletList, .orderedList, .taskList,
        .blockquote, .codeBlock,
        .callout(.info), .toggle, .table, .embed,
    ]

    /// Whether the block is edited as one line of text (paragraphs,
    /// headings, list items, quotes and code), so the toolbar's text styles
    /// apply to it.
    public var isText: Bool {
        switch self {
        case .paragraph, .heading, .bulletList, .orderedList, .taskList, .blockquote, .codeBlock: true
        case .callout, .toggle, .table, .embed: false
        }
    }
}
