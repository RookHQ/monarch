/// Which agent of a ``MonarchAiSharedSession`` is answering now, and whose request it is.
@_spi(Experimental) public struct MonarchAiSharedSessionAnswering: Sendable, Hashable {
    /// The id of the agent that is answering.
    public var participantID: String
    /// The id of the person whose request is being answered.
    public var forParticipantID: String
    /// A short description of the request, for example "Summarise the Q3 notes".
    public var label: String?

    /// An answer in progress.
    ///
    /// - Parameters:
    ///   - participantID: The id of the agent that is answering.
    ///   - forParticipantID: The id of the person whose request it is.
    ///   - label: A short description of the request.
    public init(participantID: String, forParticipantID: String, label: String? = nil) {
        self.participantID = participantID
        self.forParticipantID = forParticipantID
        self.label = label
    }
}
