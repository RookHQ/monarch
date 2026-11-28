/// A request waiting for its turn in a ``MonarchAiSharedSession``.
@_spi(Experimental) public struct MonarchAiSharedSessionRequest: Identifiable, Sendable, Hashable {
    /// A stable id, passed back from the queue actions.
    public var id: String
    /// The id of the participant who asked.
    public var participantID: String
    /// The start of the request, shown on one line.
    public var preview: String

    /// A waiting request.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - participantID: The id of the participant who asked.
    ///   - preview: The start of the request.
    public init(id: String, participantID: String, preview: String) {
        self.id = id
        self.participantID = participantID
        self.preview = preview
    }
}
