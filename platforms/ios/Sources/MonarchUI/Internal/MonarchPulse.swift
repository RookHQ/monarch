import Foundation
import MonarchTokens

/// The breathing value of loading placeholders and "thinking" marks: 0 to 1
/// and back over one shimmer period, as a smooth cosine. Components drive it
/// from a `TimelineView(.animation)` date and pass `nil` when Reduce Motion
/// is on, which holds the value still at 0.
enum MonarchPulse {
    /// One full cycle, from the shimmer duration token.
    static let period: TimeInterval = TimeInterval(MonarchTokens.Duration.shimmer) / 1000

    /// The value at `time` seconds, or 0 for `nil` (no motion).
    ///
    /// - Parameters:
    ///   - time: Seconds on any clock, such as `timeIntervalSinceReferenceDate`.
    ///   - period: The length of one cycle. The shimmer duration by default.
    ///   - offset: A share of the period to shift by, for staggered dots (0 to 1).
    /// - Returns: A value from 0 to 1.
    static func value(at time: TimeInterval?, period: TimeInterval = period, offset: Double = 0) -> Double {
        guard let time, period > 0 else { return 0 }
        let shifted = time / period + offset
        let phase = shifted - shifted.rounded(.down)
        return (1 - cos(phase * 2 * .pi)) / 2
    }
}
