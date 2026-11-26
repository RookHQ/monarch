/// One suggested change in a ``MonarchAiInlineDiff``: a run of adjacent
/// deleted and inserted segments. Returned by
/// ``MonarchAiInlineDiff/changes(in:)``.
@_spi(Experimental) public struct MonarchAiInlineDiffChange: Sendable, Hashable, Identifiable {
    /// The segments' id, or the index of the change's first segment as a string.
    public let id: String
    /// The indexes of the change's segments in the segment array.
    public let segmentIndexes: [Int]
    /// The removed text, joined.
    public let deleted: String
    /// The added text, joined.
    public let inserted: String

    /// A change.
    ///
    /// - Parameters:
    ///   - id: The id of the change.
    ///   - segmentIndexes: The indexes of its segments.
    ///   - deleted: The removed text.
    ///   - inserted: The added text.
    public init(id: String, segmentIndexes: [Int], deleted: String, inserted: String) {
        self.id = id
        self.segmentIndexes = segmentIndexes
        self.deleted = deleted
        self.inserted = inserted
    }
}
