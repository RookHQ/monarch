/// One run of a suggested edit: unchanged, inserted or deleted text, shown by
/// ``MonarchAiInlineDiff``.
///
/// Build the runs with ``words(from:to:)`` or take them from the service
/// that made the suggestion.
@_spi(Experimental) public struct MonarchAiDiffSegment: Sendable, Hashable {
    /// What happened to the text.
    public var kind: MonarchAiDiffSegmentKind
    /// The text of the run.
    public var text: String
    /// An optional id. Adjacent inserted and deleted runs with the same id
    /// (or all without one) form one change, and the change takes this id.
    public var id: String?

    /// A run of text.
    ///
    /// - Parameters:
    ///   - kind: Whether the text is unchanged, inserted or deleted.
    ///   - text: The text of the run.
    ///   - id: An optional id that names the change the run belongs to.
    public init(_ kind: MonarchAiDiffSegmentKind, text: String, id: String? = nil) {
        self.kind = kind
        self.text = text
        self.id = id
    }

    /// The word diff from `before` to `after`, as runs: each change is one
    /// deleted run followed by one inserted run, and whitespace alone never
    /// splits a change in two. The same diff as ``MonarchAiEditProposal``.
    ///
    /// - Parameters:
    ///   - before: The current text.
    ///   - after: The suggested text.
    /// - Returns: The runs, empty when both texts are empty.
    public static func words(from before: String, to after: String) -> [MonarchAiDiffSegment] {
        MonarchAiEditProposalDiff.words(before, after).map { segment in
            switch segment.kind {
            case .equal: MonarchAiDiffSegment(.equal, text: segment.text)
            case .insert: MonarchAiDiffSegment(.insert, text: segment.text)
            case .delete: MonarchAiDiffSegment(.delete, text: segment.text)
            }
        }
    }
}
