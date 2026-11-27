/// One data column of ``MonarchPreviewRows``.
public struct MonarchPreviewRowsColumn: Sendable, Hashable, Identifiable {
    /// The key of the column's values in ``MonarchPreviewRow/values``.
    public var id: String
    /// The column name, already localised.
    public var label: String
    /// Whether the values are numbers, drawn with tabular digits.
    public var isNumeric: Bool

    /// A column.
    ///
    /// - Parameters:
    ///   - label: The column name, already localised.
    ///   - id: The key of the column's values.
    ///   - isNumeric: Whether the values are numbers.
    public init(_ label: String, id: String, isNumeric: Bool = false) {
        self.label = label
        self.id = id
        self.isNumeric = isNumeric
    }
}
