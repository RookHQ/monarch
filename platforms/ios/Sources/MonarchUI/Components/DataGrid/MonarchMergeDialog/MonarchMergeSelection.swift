/// What the person chose in a ``MonarchMergeDialog``.
public struct MonarchMergeSelection: Hashable, Sendable {
    /// The record that survives.
    public let primaryID: String
    /// The other records, merged into the primary one, in their original order.
    public let mergedIDs: [String]
    /// For each field identifier, the identifier of the record whose value is kept.
    public let fields: [String: String]

    /// A selection.
    ///
    /// - Parameters:
    ///   - primaryID: The record that survives.
    ///   - mergedIDs: The records merged into it.
    ///   - fields: For each field, the record whose value is kept.
    public init(primaryID: String, mergedIDs: [String], fields: [String: String]) {
        self.primaryID = primaryID
        self.mergedIDs = mergedIDs
        self.fields = fields
    }
}
