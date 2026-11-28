import Foundation

/// A link of a ``MonarchSiteShell`` menu or a ``MonarchSiteFooter`` column.
public struct MonarchSiteLink: Sendable, Hashable, Identifiable {
    /// The link's title.
    public let label: String
    /// Where it goes.
    public let destination: URL?
    /// One line under the title, shown in menus.
    public let description: String?

    /// The title, which identifies the link in its group.
    public var id: String { label }

    /// A link.
    ///
    /// - Parameters:
    ///   - label: The title, already localised.
    ///   - destination: Where it goes.
    ///   - description: One line under the title, shown in menus.
    public init(_ label: String, destination: URL? = nil, description: String? = nil) {
        self.label = label
        self.destination = destination
        self.description = description
    }
}
