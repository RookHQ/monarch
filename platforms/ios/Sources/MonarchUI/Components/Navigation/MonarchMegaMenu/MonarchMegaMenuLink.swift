import Foundation

/// One destination of a ``MonarchMegaMenu`` panel.
public struct MonarchMegaMenuLink: Sendable, Hashable, Identifiable {
    /// The link's title.
    public let label: String
    /// Where it goes.
    public let destination: URL?
    /// One line under the title.
    public let description: String?
    /// The name of an SF Symbol shown in a tile before the title.
    public let systemImage: String?
    /// A short label after the title, such as "New".
    public let badge: String?
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
    ///   - systemImage: The name of an SF Symbol shown in a tile before the title.
    ///   - badge: A short label after the title, such as "New".
    ///   - isExternal: Whether the link leaves the app.
    public init(
        _ label: String,
        destination: URL? = nil,
        description: String? = nil,
        systemImage: String? = nil,
        badge: String? = nil,
        isExternal: Bool = false
    ) {
        self.label = label
        self.destination = destination
        self.description = description
        self.systemImage = systemImage
        self.badge = badge
        self.isExternal = isExternal
    }
}
