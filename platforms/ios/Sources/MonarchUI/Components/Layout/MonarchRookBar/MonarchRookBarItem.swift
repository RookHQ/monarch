import Foundation

/// A top-level entry of a ``MonarchRookBar``: a menu of groups, or a plain link such as Pricing.
public struct MonarchRookBarItem: Sendable, Hashable, Identifiable {
    /// A stable id.
    public let id: String
    /// The entry's title.
    public let label: String
    /// For a link: where it goes.
    public let destination: URL?
    /// For a menu: its groups.
    public let groups: [MonarchRookBarGroup]

    /// A menu.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The entry's title, already localised.
    ///   - groups: Its groups, in order.
    public init(_ id: String, label: String, groups: [MonarchRookBarGroup]) {
        self.id = id
        self.label = label
        self.destination = nil
        self.groups = groups
    }

    /// A link.
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
    }
}
