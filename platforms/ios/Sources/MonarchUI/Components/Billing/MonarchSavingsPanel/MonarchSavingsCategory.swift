/// A group of unused items in a ``MonarchSavingsPanel``, such as "Parked"
/// or "No website".
@_spi(Experimental) public struct MonarchSavingsCategory: Identifiable, Sendable, Hashable {
    /// A stable id, also the status key looked up for the badge.
    public let id: String
    /// How many items are in the group. A group with none is not shown.
    public let count: Int
    /// The visible label. Default: the status registry's label for `id`.
    public let label: String?

    /// A group of unused items.
    ///
    /// - Parameters:
    ///   - id: A stable id, also the status key of the badge.
    ///   - count: How many items are in the group.
    ///   - label: The visible label, already localised.
    public init(id: String, count: Int, label: String? = nil) {
        self.id = id
        self.count = count
        self.label = label
    }
}
