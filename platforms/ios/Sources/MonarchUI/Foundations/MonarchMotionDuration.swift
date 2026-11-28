import Foundation
import MonarchTokens

/// How long a transition takes.
public enum MonarchMotionDuration: Sendable, Hashable, CaseIterable {
    /// No time. The change is immediate.
    case instant
    /// Press and hover feedback.
    case fast
    /// Most transitions.
    case base
    /// Larger surfaces.
    case slow
    /// The longest transition.
    case slower

    /// The duration in seconds.
    public var seconds: TimeInterval {
        let milliseconds =
            switch self {
            case .instant: MonarchTokens.Duration.instant
            case .fast: MonarchTokens.Duration.fast
            case .base: MonarchTokens.Duration.base
            case .slow: MonarchTokens.Duration.slow
            case .slower: MonarchTokens.Duration.slower
            }
        return TimeInterval(milliseconds) / 1000
    }
}
