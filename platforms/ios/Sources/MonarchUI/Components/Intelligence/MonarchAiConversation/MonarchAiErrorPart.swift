/// A failure of the turn, as the last part of the reply.
public struct MonarchAiErrorPart: Sendable, Hashable, Codable {
    /// The error code, for example `network` or `http-503`. Never shown.
    public var code: String
    /// The message for the person.
    public var message: String
    /// Whether the turn may be tried again unchanged.
    public var isRetryable: Bool
    /// Milliseconds until a retry is allowed, when the service said.
    public var retryAfterMs: Int?
    /// Whether the connection dropped mid-reply but the run can be picked up
    /// where the service has it, with `MonarchAiConversation.resumeRun(_:)`.
    public var isResumable: Bool

    /// An error part.
    ///
    /// - Parameters:
    ///   - code: The error code.
    ///   - message: The message for the person.
    ///   - isRetryable: Whether the turn may be tried again unchanged.
    ///   - retryAfterMs: Milliseconds until a retry is allowed.
    ///   - isResumable: Whether the run can be resumed.
    public init(code: String, message: String, isRetryable: Bool, retryAfterMs: Int? = nil, isResumable: Bool = false) {
        self.code = code
        self.message = message
        self.isRetryable = isRetryable
        self.retryAfterMs = retryAfterMs
        self.isResumable = isResumable
    }

    private enum CodingKeys: String, CodingKey {
        case code
        case message
        case isRetryable = "retryable"
        case retryAfterMs
        case isResumable = "resumable"
    }

    /// Decodes the web's shape, where `resumable` is left out unless it is true.
    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        code = try container.decode(String.self, forKey: .code)
        message = try container.decode(String.self, forKey: .message)
        isRetryable = try container.decode(Bool.self, forKey: .isRetryable)
        retryAfterMs = try container.decodeIfPresent(Int.self, forKey: .retryAfterMs)
        isResumable = try container.decodeIfPresent(Bool.self, forKey: .isResumable) ?? false
    }

    /// Encodes the web's shape.
    public func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(code, forKey: .code)
        try container.encode(message, forKey: .message)
        try container.encode(isRetryable, forKey: .isRetryable)
        try container.encodeIfPresent(retryAfterMs, forKey: .retryAfterMs)
        if isResumable { try container.encode(true, forKey: .isResumable) }
    }
}
