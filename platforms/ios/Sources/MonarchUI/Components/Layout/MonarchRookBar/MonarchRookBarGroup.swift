/// A group of links in a ``MonarchRookBar`` menu, such as an app category with its promise, or the recent posts.
public struct MonarchRookBarGroup: Sendable, Hashable, Identifiable {
    /// The group's title, such as "Sales & Support".
    public let title: String?
    /// One line under the title, such as "Win customers, then look after them."
    public let description: String?
    /// The links, in order.
    public let links: [MonarchRookBarLink]

    /// The title, else the first link's title, which identifies the group in its menu.
    public var id: String { title ?? links.first?.label ?? "" }

    /// A group.
    ///
    /// - Parameters:
    ///   - title: The group's title, already localised. Without one the links follow the previous group directly.
    ///   - description: One line under the title.
    ///   - links: The links, in order.
    public init(_ title: String? = nil, description: String? = nil, links: [MonarchRookBarLink]) {
        self.title = title
        self.description = description
        self.links = links
    }
}
