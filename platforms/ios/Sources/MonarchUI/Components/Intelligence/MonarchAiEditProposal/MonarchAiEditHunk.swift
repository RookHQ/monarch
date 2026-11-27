/// One self-contained change of a ``MonarchAiEditProposal``: a field, a
/// paragraph or a sentence, before and after.
@_spi(Experimental) public struct MonarchAiEditHunk: Identifiable, Sendable, Hashable {
    /// A stable id, passed to the accept and reject actions.
    public var id: String
    /// The current text. Empty for an addition.
    public var before: String
    /// The text the AI proposes. Empty for a removal.
    public var after: String
    /// What the change touches, for example a field name or "Paragraph 2".
    public var label: String?
    /// The decision, when the app keeps it. `nil` lets the component remember decisions itself.
    public var status: MonarchAiEditHunkStatus?

    /// A change.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - before: The current text.
    ///   - after: The proposed text.
    ///   - label: What the change touches.
    ///   - status: The decision, when the app keeps it.
    public init(
        id: String,
        before: String,
        after: String,
        label: String? = nil,
        status: MonarchAiEditHunkStatus? = nil
    ) {
        self.id = id
        self.before = before
        self.after = after
        self.label = label
        self.status = status
    }
}
