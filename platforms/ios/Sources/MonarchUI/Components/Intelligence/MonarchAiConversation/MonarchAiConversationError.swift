import Foundation

/// Why the last turn of a conversation failed.
public struct MonarchAiConversationError: Sendable, Hashable {
    /// The error code, for example `network`, `rate-limited` or `http-503`. Never show it;
    /// `MonarchAiErrorKind(code:)` maps it to a kind with plain words.
    public var code: String
    /// The message for the person.
    public var message: String
    /// Whether the turn may be tried again unchanged.
    public var isRetryable: Bool
    /// Milliseconds until a retry is allowed, when the service said.
    public var retryAfterMs: Int?
    /// Whether the connection dropped mid-reply and the run can be picked up with
    /// `resumeRun(_:)` rather than asked again.
    public var isResumable: Bool

    /// An error.
    ///
    /// - Parameters:
    ///   - code: The error code.
    ///   - message: The message for the person.
    ///   - isRetryable: Whether the turn may be tried again unchanged.
    ///   - retryAfterMs: Milliseconds until a retry is allowed.
    ///   - isResumable: Whether the run can be resumed.
    public init(
        code: String,
        message: String,
        isRetryable: Bool,
        retryAfterMs: Int? = nil,
        isResumable: Bool = false
    ) {
        self.code = code
        self.message = message
        self.isRetryable = isRetryable
        self.retryAfterMs = retryAfterMs
        self.isResumable = isResumable
    }

    private static let retryableCodes: Set<String> = [
        "network", "timeout", "rate-limited", "rate-limit", "server", "overloaded", "overloaded_error",
        "rate_limit_error", "api_error",
    ]

    /// Whether a failed turn with this code may be tried again unchanged:
    /// network drops, timeouts, rate limits, overload and server errors
    /// (`http-5xx`). Unknown codes are not retryable, so a broken request is
    /// not repeated.
    public static func isRetryable(code: String?) -> Bool {
        guard let code, !code.isEmpty else { return false }
        if retryableCodes.contains(code) { return true }
        let prefix = "http-"
        guard code.hasPrefix(prefix), code.count == prefix.count + 3, let status = Int(code.dropFirst(prefix.count))
        else { return false }
        return status >= 500
    }
}
