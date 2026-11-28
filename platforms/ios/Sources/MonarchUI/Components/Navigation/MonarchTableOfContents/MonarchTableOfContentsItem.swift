/// One entry of ``MonarchTableOfContents``: a heading of the page.
///
/// ```swift
/// MonarchTableOfContentsItem("Billing", id: Section.billing)
/// MonarchTableOfContentsItem("Invoices", id: Section.invoices, level: 3)
/// ```
public struct MonarchTableOfContentsItem<ID: Hashable>: Identifiable {
    /// The identity of the section, the same value given to
    /// `monarchTableOfContentsTarget(_:)` on its heading.
    public let id: ID
    /// The entry's text, usually the heading, already localised.
    public let title: String
    /// The heading level: 2 for a section, 3 for a subsection. A deeper level is indented
    /// under the entry before it.
    public let level: Int

    /// An entry.
    ///
    /// - Parameters:
    ///   - title: The entry's text, usually the heading, already localised.
    ///   - id: The identity of the section.
    ///   - level: The heading level. Deeper levels nest under the entry before them.
    public init(_ title: String, id: ID, level: Int = 2) {
        self.title = title
        self.id = id
        self.level = level
    }
}

extension MonarchTableOfContentsItem: Sendable where ID: Sendable {}
extension MonarchTableOfContentsItem: Hashable {}
