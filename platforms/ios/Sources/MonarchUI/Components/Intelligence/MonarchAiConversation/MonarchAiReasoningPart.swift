/// The model's reasoning before or between its answers.
public struct MonarchAiReasoningPart: Sendable, Hashable, Codable {
    /// The reasoning so far.
    public var text: String
    /// Whether more reasoning is arriving.
    public var state: MonarchAiPartState
    /// How long the model reasoned, in milliseconds, once known.
    public var durationMs: Int?
    /// Whether the reasoning exists but its text is not shown.
    public var isRedacted: Bool

    /// A reasoning part.
    ///
    /// - Parameters:
    ///   - text: The reasoning.
    ///   - state: Whether more reasoning is arriving.
    ///   - durationMs: How long the model reasoned, in milliseconds.
    ///   - isRedacted: Whether the reasoning exists but its text is not shown.
    public init(text: String, state: MonarchAiPartState = .done, durationMs: Int? = nil, isRedacted: Bool = false) {
        self.text = text
        self.state = state
        self.durationMs = durationMs
        self.isRedacted = isRedacted
    }

    private enum CodingKeys: String, CodingKey {
        case text
        case state
        case durationMs
        case isRedacted = "redacted"
    }

    /// Decodes the web's shape, where `redacted` is left out unless it is true.
    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        text = try container.decode(String.self, forKey: .text)
        state = try container.decode(MonarchAiPartState.self, forKey: .state)
        durationMs = try container.decodeIfPresent(Int.self, forKey: .durationMs)
        isRedacted = try container.decodeIfPresent(Bool.self, forKey: .isRedacted) ?? false
    }

    /// Encodes the web's shape.
    public func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(text, forKey: .text)
        try container.encode(state, forKey: .state)
        try container.encodeIfPresent(durationMs, forKey: .durationMs)
        if isRedacted { try container.encode(true, forKey: .isRedacted) }
    }
}
