/// A titled group of links in a ``MonarchMegaMenu`` panel.
public struct MonarchMegaMenuGroup: Sendable, Hashable, Identifiable {
    /// The group's heading, shown in small capitals.
    public let heading: String
    /// The links, in order.
    public let links: [MonarchMegaMenuLink]

    /// The heading, which identifies the group in its panel.
    public var id: String { heading }

    /// A group.
    ///
    /// - Parameters:
    ///   - heading: The group's heading, already localised.
    ///   - links: The links, in order.
    public init(_ heading: String, links: [MonarchMegaMenuLink]) {
        self.heading = heading
        self.links = links
    }
}
