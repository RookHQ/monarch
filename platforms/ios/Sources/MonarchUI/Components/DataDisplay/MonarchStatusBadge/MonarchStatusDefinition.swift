/// How one status looks and reads in a ``MonarchStatusBadge``.
public struct MonarchStatusDefinition: Sendable, Hashable {
    /// The words shown and read, such as "Partial outage". Never a raw key.
    public let label: String
    /// The meaning, which selects the colours.
    public let tone: MonarchTone
    /// An SF Symbol with a shape of its own, so colour is never the only
    /// signal. `nil` for none.
    public let systemImage: String?
    /// One line explaining the status, for a legend the app draws. Not shown
    /// by the badge.
    public let description: String?

    /// A status.
    ///
    /// - Parameters:
    ///   - label: The words shown and read, already localised.
    ///   - tone: The meaning, which selects the colours.
    ///   - systemImage: A decorative SF Symbol before the label.
    ///   - description: One line explaining the status, for a legend.
    public init(_ label: String, tone: MonarchTone, systemImage: String? = nil, description: String? = nil) {
        self.label = label
        self.tone = tone
        self.systemImage = systemImage
        self.description = description
    }
}
