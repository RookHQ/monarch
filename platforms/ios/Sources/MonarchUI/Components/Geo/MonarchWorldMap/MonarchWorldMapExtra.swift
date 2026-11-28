/// A value in ``MonarchWorldMap`` that belongs to no single country, such
/// as "Global" or "EU". It is shown as a tile under the map and as a row of
/// the table.
public struct MonarchWorldMapExtra: Sendable, Hashable, Identifiable {
    /// A stable key, such as `global`.
    public let key: String
    /// The name, such as "Global".
    public let label: String
    /// The value.
    public let value: Double

    /// The identity of the extra, which is its key.
    public var id: String { key }

    /// A value that belongs to no country.
    ///
    /// - Parameters:
    ///   - label: The name, already localised.
    ///   - value: The value.
    ///   - key: A stable key. The label by default.
    public init(_ label: String, value: Double, key: String? = nil) {
        self.key = key ?? label
        self.label = label
        self.value = value
    }
}
