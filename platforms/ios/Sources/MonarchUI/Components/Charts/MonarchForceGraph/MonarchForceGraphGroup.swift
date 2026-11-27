/// A group of nodes of a ``MonarchForceGraph``, drawn in one data colour and
/// listed in the legend.
public struct MonarchForceGraphGroup: Sendable, Hashable, Identifiable {
    /// The key nodes refer to.
    public let key: String
    /// The name in the legend, the table and for VoiceOver, already localised.
    public let label: String
    /// The colour slot, or `nil` for the slot of the group's position.
    public let color: MonarchChartColor?

    /// The key.
    public var id: String { key }

    /// A group.
    ///
    /// - Parameters:
    ///   - key: The key nodes refer to.
    ///   - label: The name, already localised.
    ///   - color: The colour slot. The slot of the group's position when `nil`; the sixth and later are neutral.
    public init(_ key: String, label: String, color: MonarchChartColor? = nil) {
        self.key = key
        self.label = label
        self.color = color
    }
}
