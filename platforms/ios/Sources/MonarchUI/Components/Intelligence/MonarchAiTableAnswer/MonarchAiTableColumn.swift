/// A column of ``MonarchAiTableAnswer``: which value of each row it shows,
/// its header, and how it compares and aligns.
public struct MonarchAiTableColumn: Identifiable, Sendable, Hashable {
    /// The key of the value in each row.
    public var key: String
    /// The header text. `nil` shows the key.
    public var label: String?
    /// How values compare and format. `nil` infers it from the values.
    public var type: MonarchAiTableColumnType?
    /// Where values sit. `nil` puts numbers at the trailing edge and the rest at the leading edge.
    public var alignment: MonarchAiTableColumnAlignment?

    /// The key, which identifies the column.
    public var id: String { key }

    /// A column.
    ///
    /// - Parameters:
    ///   - key: The key of the value in each row.
    ///   - label: The header text, already localised. `nil` shows the key.
    ///   - type: How values compare and format. `nil` infers it from the values.
    ///   - alignment: Where values sit. `nil` puts numbers at the trailing edge.
    public init(
        _ key: String,
        label: String? = nil,
        type: MonarchAiTableColumnType? = nil,
        alignment: MonarchAiTableColumnAlignment? = nil
    ) {
        self.key = key
        self.label = label
        self.type = type
        self.alignment = alignment
    }
}
