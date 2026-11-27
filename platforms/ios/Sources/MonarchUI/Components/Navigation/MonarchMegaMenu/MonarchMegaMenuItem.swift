import Foundation

/// A top-level entry of a ``MonarchMegaMenu``: a menu that opens a panel of groups, or a plain link.
public struct MonarchMegaMenuItem: Sendable, Hashable, Identifiable {
    /// A stable id, used by the open-item binding.
    public let id: String
    /// The entry's title.
    public let label: String
    /// For a plain link: where it goes.
    public let destination: URL?
    /// For a menu: the groups of its panel.
    public let groups: [MonarchMegaMenuGroup]
    /// For a menu: a highlighted card under the groups.
    public let featured: MonarchMegaMenuLink?

    /// Whether the entry opens a panel.
    var isMenu: Bool { !groups.isEmpty || featured != nil }

    /// A menu that opens a panel.
    ///
    /// - Parameters:
    ///   - id: A stable id, used by the open-item binding.
    ///   - label: The entry's title, already localised.
    ///   - groups: The groups of the panel.
    ///   - featured: A highlighted card under the groups, such as a tour of what changed.
    public init(_ id: String, label: String, groups: [MonarchMegaMenuGroup], featured: MonarchMegaMenuLink? = nil) {
        self.id = id
        self.label = label
        self.destination = nil
        self.groups = groups
        self.featured = featured
    }

    /// A plain link.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The entry's title, already localised.
    ///   - destination: Where it goes.
    public init(_ id: String, label: String, destination: URL?) {
        self.id = id
        self.label = label
        self.destination = destination
        self.groups = []
        self.featured = nil
    }
}
