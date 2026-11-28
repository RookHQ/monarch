/// One value of a ``MonarchHeatmap``. A row and column pair without a cell is "no data".
public struct MonarchHeatmapCell: Sendable, Hashable {
    /// The key of the row.
    public let row: String
    /// The key of the column.
    public let column: String
    /// The value; `nil` for no data.
    public let value: Double?
    /// Shown instead of the formatted value, such as "7h 30m", already localised.
    public let valueLabel: String?
    /// A short note after the value, such as "Holiday", already localised.
    public let note: String?

    /// A cell.
    ///
    /// - Parameters:
    ///   - row: The key of the row.
    ///   - column: The key of the column.
    ///   - value: The value; `nil` for no data.
    ///   - valueLabel: Shown instead of the formatted value.
    ///   - note: A short note after the value.
    public init(row: String, column: String, value: Double?, valueLabel: String? = nil, note: String? = nil) {
        self.row = row
        self.column = column
        self.value = value
        self.valueLabel = valueLabel
        self.note = note
    }
}
