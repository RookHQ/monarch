/// Text of a message.
public struct MonarchAiTextPart: Sendable, Hashable, Codable {
    /// The text so far.
    public var text: String
    /// Whether more text is arriving.
    public var state: MonarchAiPartState

    /// A text part.
    ///
    /// - Parameters:
    ///   - text: The text.
    ///   - state: Whether more text is arriving.
    public init(text: String, state: MonarchAiPartState = .done) {
        self.text = text
        self.state = state
    }
}
