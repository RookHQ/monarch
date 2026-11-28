import Foundation

/// A top-level entry of a ``MonarchSiteShell`` menu: a link, or a submenu of links.
public struct MonarchSiteNavItem: Sendable, Hashable, Identifiable {
    /// The entry's title.
    public let label: String
    /// For a link: where it goes.
    public let destination: URL?
    /// For a submenu: its links.
    public let links: [MonarchSiteLink]

    /// The title, which identifies the entry in the menu.
    public var id: String { label }

    /// A link.
    ///
    /// - Parameters:
    ///   - label: The entry's title, already localised.
    ///   - destination: Where it goes.
    public init(_ label: String, destination: URL?) {
        self.label = label
        self.destination = destination
        self.links = []
    }

    /// A submenu.
    ///
    /// - Parameters:
    ///   - label: The entry's title, already localised.
    ///   - links: Its links, in order.
    public init(_ label: String, links: [MonarchSiteLink]) {
        self.label = label
        self.destination = nil
        self.links = links
    }
}
