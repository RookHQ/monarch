/// One figure of a ``MonarchStatStrip``: the value as written and what it counts.
public struct MonarchStatStripItem: Sendable, Hashable {
    /// The value exactly as shown, such as "13".
    public let value: String
    /// What the value counts, such as "Interface languages".
    public let label: String

    /// A stat.
    ///
    /// - Parameters:
    ///   - value: The value exactly as shown. It is never formatted.
    ///   - label: What the value counts, already localised.
    public init(_ value: String, label: String) {
        self.value = value
        self.label = label
    }
}
