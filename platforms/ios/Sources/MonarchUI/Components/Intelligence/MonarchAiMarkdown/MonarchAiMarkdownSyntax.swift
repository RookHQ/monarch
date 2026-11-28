/// An inline token inside a paragraph, heading, table cell or link, as the
/// web parser produces it.
enum MonarchAiMarkdownInline: Equatable, Sendable {
    /// Plain text. A soft line break inside a paragraph is kept as `\n`.
    case text(String)
    /// An inline code span.
    case code(String)
    /// Inline maths source written as `\(…\)` or `$$…$$` inside a line.
    case math(String)
    /// Emphasis, strong emphasis or strikethrough around the children.
    case styled(MonarchAiMarkdownEmphasis, [MonarchAiMarkdownInline])
    /// A link. `autolink` is true for `<https://…>` and bare `https://…` or `www.…` links.
    case link(href: String, title: String?, autolink: Bool, children: [MonarchAiMarkdownInline])
    /// An image; shown as its alt text unless the app renders images.
    case image(src: String, alt: String, title: String?)
    /// A hard line break.
    case lineBreak
    /// A footnote reference, `[^label]`, with the label lower-cased.
    case footnoteReference(String)
}

/// The three kinds of delimited inline style.
enum MonarchAiMarkdownEmphasis: Equatable, Sendable {
    case emphasis
    case strong
    case strikethrough
}

/// The alignment of a table column from its delimiter row. `nil` in a list means none.
enum MonarchAiMarkdownAlignment: Equatable, Sendable {
    case leading
    case center
    case trailing
}

/// One list item: `checked` is `true` or `false` for a task item, `nil` otherwise.
struct MonarchAiMarkdownListItem: Equatable, Sendable {
    let checked: Bool?
    let children: [MonarchAiMarkdownBlock]
}

/// A table: the column alignments, the header cells and the body rows.
struct MonarchAiMarkdownTableContent: Equatable, Sendable {
    let alignments: [MonarchAiMarkdownAlignment?]
    let header: [[MonarchAiMarkdownInline]]
    let rows: [[[MonarchAiMarkdownInline]]]
}

/// A fenced, indented or display-maths code block. `language` is the first
/// word of the info string, lower-cased, or `math` for display maths;
/// `isClosed` is false while the closing fence has not arrived.
struct MonarchAiMarkdownCode: Equatable, Sendable {
    let language: String
    let info: String
    let filename: String?
    let code: String
    let isClosed: Bool
}

/// A block token. Top-level blocks are independent of the ones after them.
enum MonarchAiMarkdownBlock: Equatable, Sendable {
    case heading(level: Int, content: [MonarchAiMarkdownInline])
    case paragraph([MonarchAiMarkdownInline])
    case code(MonarchAiMarkdownCode)
    case quote([MonarchAiMarkdownBlock])
    /// A list. A loose list separates its items' paragraphs; a tight one does not.
    case list(ordered: Bool, start: Int, isLoose: Bool, items: [MonarchAiMarkdownListItem])
    case table(MonarchAiMarkdownTableContent)
    case rule
    case footnoteDefinition(label: String, children: [MonarchAiMarkdownBlock])

    /// Whether the block is a heading.
    var isHeading: Bool {
        if case .heading = self { return true }
        return false
    }

    /// Whether the block is a footnote definition, which is listed at the end.
    var isFootnoteDefinition: Bool {
        if case .footnoteDefinition = self { return true }
        return false
    }
}

/// The result of an incremental parse; passed back in on the next update.
struct MonarchAiMarkdownParseState: Equatable, Sendable {
    let text: String
    let blocks: [MonarchAiMarkdownBlock]
    /// UTF-16 offset of each block's first line.
    let offsets: [Int]
}
