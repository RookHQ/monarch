import Foundation

/// One block of a ``MonarchBlockEditor`` document: a paragraph, a heading, a
/// list item, a quote, code, a callout, a toggle, a table or an embed.
///
/// A document is an array of blocks; the app maps it to its own storage
/// format (HTML, Markdown or JSON).
@_spi(Experimental) public struct MonarchBlock: Identifiable, Sendable, Hashable {
    /// A stable identifier, unique in the document.
    public var id: String
    /// What the block is.
    public var kind: MonarchBlockKind
    /// The text: the paragraph, heading, list item, quote, code or callout
    /// text, the summary of a toggle, or the link of an embed.
    public var text: String
    /// The content of a toggle, or the caption of an embed. Empty for other kinds.
    public var detail: String
    /// Whether a checklist item is ticked.
    public var isChecked: Bool
    /// Whether a toggle shows its content.
    public var isExpanded: Bool
    /// The cells of a table block. A table block without data shows an empty 3 by 3 table.
    public var table: MonarchTableBlockData?

    /// A block.
    ///
    /// - Parameters:
    ///   - id: A stable identifier, unique in the document. A new UUID by default.
    ///   - kind: What the block is.
    ///   - text: The text, summary or link.
    ///   - detail: The content of a toggle or the caption of an embed.
    ///   - isChecked: Whether a checklist item is ticked.
    ///   - isExpanded: Whether a toggle shows its content.
    ///   - table: The cells of a table block.
    public init(
        id: String = UUID().uuidString,
        kind: MonarchBlockKind = .paragraph,
        text: String = "",
        detail: String = "",
        isChecked: Bool = false,
        isExpanded: Bool = true,
        table: MonarchTableBlockData? = nil
    ) {
        self.id = id
        self.kind = kind
        self.text = text
        self.detail = detail
        self.isChecked = isChecked
        self.isExpanded = isExpanded
        self.table = table
    }
}
