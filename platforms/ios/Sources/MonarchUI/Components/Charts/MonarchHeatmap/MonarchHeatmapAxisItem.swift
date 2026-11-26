/// A row or column of a ``MonarchHeatmap``.
public struct MonarchHeatmapAxisItem: Sendable, Hashable, Identifiable {
    /// A stable key, matched by ``MonarchHeatmapCell/row`` and ``MonarchHeatmapCell/column``.
    public let key: String
    /// The text on the axis, in the table and for VoiceOver, already localised.
    public let label: String

    /// The key.
    public var id: String { key }

    /// A row or column.
    ///
    /// - Parameters:
    ///   - label: The text on the axis, already localised.
    ///   - key: A stable key. The label when `nil`.
    public init(_ label: String, key: String? = nil) {
        self.label = label
        self.key = key ?? label
    }
}
