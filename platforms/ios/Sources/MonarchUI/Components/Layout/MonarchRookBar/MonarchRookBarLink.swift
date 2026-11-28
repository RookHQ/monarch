import Foundation

/// A link in a ``MonarchRookBar`` menu: an app, a service, a page or a post.
public struct MonarchRookBarLink: Sendable, Hashable, Identifiable {
    /// The link's title.
    public let label: String
    /// Where it goes.
    public let destination: URL?
    /// One line under the title.
    public let description: String?
    /// A short line for a post, such as its kind and date.
    public let meta: String?
    /// Whether the link leaves the app (another site or app). It gets an outward arrow.
    public let isExternal: Bool

    /// The title, which identifies the link in its group.
    public var id: String { label }

    /// A link.
    ///
    /// - Parameters:
    ///   - label: The title, already localised.
    ///   - destination: Where it goes.
    ///   - description: One line under the title.
    ///   - meta: A short line for a post, such as "Blog · Mar 2, 2026".
    ///   - isExternal: Whether the link leaves the app.
    public init(
        _ label: String,
        destination: URL? = nil,
        description: String? = nil,
        meta: String? = nil,
        isExternal: Bool = false
    ) {
        self.label = label
        self.destination = destination
        self.description = description
        self.meta = meta
        self.isExternal = isExternal
    }
}
