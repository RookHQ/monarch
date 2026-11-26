/// An extra line for a country in ``MonarchWorldMap``, such as a second
/// metric. It is read with the country and becomes a column of the table.
public struct MonarchWorldMapDetail: Sendable, Hashable {
    /// The name of the line, such as "Users".
    public let label: String
    /// The value as written, such as "340".
    public let value: String

    /// A detail line.
    ///
    /// - Parameters:
    ///   - label: The name of the line, already localised.
    ///   - value: The value, already formatted.
    public init(_ label: String, value: String) {
        self.label = label
        self.value = value
    }
}
