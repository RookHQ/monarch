import Foundation

/// Why an assistant turn failed.
public enum MonarchAiErrorKind: Sendable, Hashable, CaseIterable {
    /// Too many requests, a quota, or an overloaded service.
    case rateLimit
    /// The conversation no longer fits the model's context window.
    case contextOverflow
    /// A safety filter blocked the request or the model refused.
    case safetyBlock
    /// The service could not be reached, or the connection dropped or timed out.
    case network
    /// A tool the assistant used failed.
    case toolFailure
    /// Anything else.
    case unknown

    /// The kind for a raw error code.
    ///
    /// `429` and `rate_limit_exceeded` give `rateLimit`, `context_length_exceeded`
    /// and `413` give `contextOverflow`, `content_filter` gives `safetyBlock`,
    /// `ETIMEDOUT` and `503` give `network`, `tool_error` gives `toolFailure`,
    /// and anything else gives `unknown`. Case, underscores and spaces are
    /// ignored, and codes such as `http-429` are understood. The code is
    /// never shown.
    ///
    /// - Parameter code: An HTTP status, a provider's error code, or the code of an error part.
    public init(code: String?) {
        self = Self.rules.first { MonarchAiErrorKind.matches(code, $0.pattern) }?.kind ?? .unknown
    }

    /// The rules, tried in order. The patterns match a normalised code.
    private static let rules: [(kind: MonarchAiErrorKind, pattern: String)] = [
        (.rateLimit, "(^|-)(429|529)$|rate-?limit|too-many-requests|quota|overloaded|throttl"),
        (
            .contextOverflow,
            "context-(length|overflow|window|limit)|(^|-)413$|token-limit|too-many-tokens|prompt-too-long|max-context"
        ),
        (.safetyBlock, "safety|content-(filter|policy)|moderation|policy|refusal|blocked"),
        (.toolFailure, "tool"),
        (
            .network,
            "network|offline|timeout|timedout|timed-out|connection|econn|fetch-failed|unreachable"
                + "|(^|-)(0|408|502|503|504)$"
        ),
    ]

    /// A code in lower case with runs of spaces and separators as one hyphen.
    static func normalised(_ code: String) -> String {
        code.trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .replacingOccurrences(of: "[\\s_.:/]+", with: "-", options: .regularExpression)
    }

    private static func matches(_ code: String?, _ pattern: String) -> Bool {
        guard let code, !code.isEmpty else { return false }
        return normalised(code).range(of: pattern, options: .regularExpression) != nil
    }
}
