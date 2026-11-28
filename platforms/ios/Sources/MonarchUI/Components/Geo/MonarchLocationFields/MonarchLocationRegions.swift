/// A country's regions from a ``MonarchLocationAdapter``, with the country's
/// own word for them.
public struct MonarchLocationRegions: Hashable, Sendable {
    /// The regions, in display order. Empty gives a text field.
    public let regions: [MonarchRegion]
    /// The country's word for its regions, such as "Emirate", already localised.
    public let label: String?

    /// A country's regions.
    ///
    /// - Parameters:
    ///   - regions: The regions, in display order. Empty gives a text field.
    ///   - label: The country's word for its regions.
    public init(_ regions: [MonarchRegion], label: String? = nil) {
        self.regions = regions
        self.label = label
    }
}
