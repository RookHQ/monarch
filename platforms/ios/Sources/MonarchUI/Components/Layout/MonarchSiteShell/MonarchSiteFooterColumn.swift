/// A titled column of links in a ``MonarchSiteFooter``.
public struct MonarchSiteFooterColumn: Sendable, Hashable, Identifiable {
    /// The column title.
    public let title: String
    /// The links, in order.
    public let links: [MonarchSiteLink]

    /// The title, which identifies the column in the footer.
    public var id: String { title }

    /// A column.
    ///
    /// - Parameters:
    ///   - title: The column title, already localised.
    ///   - links: The links, in order.
    public init(_ title: String, links: [MonarchSiteLink]) {
        self.title = title
        self.links = links
    }
}
