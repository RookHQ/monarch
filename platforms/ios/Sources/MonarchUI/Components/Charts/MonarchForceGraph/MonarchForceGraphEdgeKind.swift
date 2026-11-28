/// A kind of link of a ``MonarchForceGraph``, listed in a legend with its line style.
public struct MonarchForceGraphEdgeKind: Sendable, Hashable, Identifiable {
    /// The key links refer to.
    public let key: String
    /// The name in the legend, the table and for VoiceOver, already localised.
    public let label: String
    /// The colour slot. Neutral when `nil`.
    public let color: MonarchChartColor?
    /// Whether the line is dashed, a second encoding besides colour.
    public let isDashed: Bool

    /// The key.
    public var id: String { key }

    /// A kind of link.
    ///
    /// - Parameters:
    ///   - key: The key links refer to.
    ///   - label: The name, already localised.
    ///   - color: The colour slot. Neutral when `nil`.
    ///   - isDashed: Whether the line is dashed.
    public init(_ key: String, label: String, color: MonarchChartColor? = nil, isDashed: Bool = false) {
        self.key = key
        self.label = label
        self.color = color
        self.isDashed = isDashed
    }
}
