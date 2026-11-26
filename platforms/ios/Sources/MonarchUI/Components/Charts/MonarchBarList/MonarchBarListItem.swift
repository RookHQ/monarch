/// One row of a ``MonarchBarList``.
public struct MonarchBarListItem: Identifiable, Sendable, Hashable {
    /// A stable identity. The label unless another is given.
    public let id: String
    /// What the row is, such as a page path. Already localised.
    public let label: String
    /// The measured value. `nil` shows a dash, read as "No value", and no bar.
    public let value: Double?
    /// A second, muted line under the label.
    public let description: String?
    /// A decorative SF Symbol before the label.
    public let systemImage: String?
    /// Shown instead of the formatted value, such as "12% · 4 of 33".
    public let displayValue: String?
    /// Whether the row is emphasised. Say why in the label too: the emphasis is not spoken.
    public let isHighlighted: Bool

    /// A row.
    ///
    /// - Parameters:
    ///   - label: What the row is, already localised.
    ///   - id: A stable identity. The label when `nil`.
    ///   - value: The measured value, or `nil` when there is none.
    ///   - description: A second, muted line under the label.
    ///   - systemImage: A decorative SF Symbol before the label.
    ///   - displayValue: Shown instead of the formatted value.
    ///   - isHighlighted: Whether the row is emphasised.
    public init(
        _ label: String,
        id: String? = nil,
        value: Double?,
        description: String? = nil,
        systemImage: String? = nil,
        displayValue: String? = nil,
        isHighlighted: Bool = false
    ) {
        self.id = id ?? label
        self.label = label
        self.value = value
        self.description = description
        self.systemImage = systemImage
        self.displayValue = displayValue
        self.isHighlighted = isHighlighted
    }
}
