/// One feature of a ``MonarchFeatureGrid``: a title, a description and an optional SF Symbol.
public struct MonarchFeatureGridItem: Sendable, Hashable {
    /// The feature's name.
    public let title: String
    /// One sentence on what it does.
    public let description: String?
    /// The name of an SF Symbol shown above the title.
    public let systemImage: String?

    /// A feature.
    ///
    /// - Parameters:
    ///   - title: The feature's name, already localised.
    ///   - description: One sentence on what it does.
    ///   - systemImage: The name of an SF Symbol shown above the title.
    public init(_ title: String, description: String? = nil, systemImage: String? = nil) {
        self.title = title
        self.description = description
        self.systemImage = systemImage
    }
}
