/// An edit the reply proposes to something outside the conversation.
public struct MonarchAiEditProposalPart: Identifiable, Sendable, Hashable, Codable {
    /// The id of the proposal.
    public var id: String
    /// What is being edited, for example a document id.
    public var target: String
    /// The change, in the shape the product uses.
    public var diff: MonarchAiJSON
    /// What happened to the proposal.
    public var state: MonarchAiEditProposalState

    /// An edit proposal.
    ///
    /// - Parameters:
    ///   - id: The id of the proposal.
    ///   - target: What is being edited.
    ///   - diff: The change.
    ///   - state: What happened to the proposal.
    public init(id: String, target: String, diff: MonarchAiJSON, state: MonarchAiEditProposalState = .proposed) {
        self.id = id
        self.target = target
        self.diff = diff
        self.state = state
    }
}
