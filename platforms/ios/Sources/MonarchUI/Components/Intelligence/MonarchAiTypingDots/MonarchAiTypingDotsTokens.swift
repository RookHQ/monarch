import Foundation
import MonarchTokens

/// Component tokens for ``MonarchAiTypingDots``.
enum MonarchAiTypingDotsTokens {
    /// The number of dots.
    static let count = 3
    /// The colour role of the dots.
    static var color: MonarchColorRole { \.aiStrong }
    /// The gap between dots, as a part of the dot size.
    static let gapRatio: CGFloat = 0.6
    /// The padding above and below the dots, as a part of the dot size.
    static let paddingRatio: CGFloat = 0.5
    /// How far a dot rises at its peak, as a part of the dot size.
    static let liftRatio: CGFloat = 0.6
    /// The opacity of a dot at rest.
    static let restingOpacity: Double = 0.4
    /// The opacity of every dot in the still form, for Reduce Motion.
    static let stillOpacity: Double = 0.7
    /// The part of a cycle a dot spends rising, and the same again falling.
    static let riseFraction: Double = 0.3
    /// The delay of each dot after the one before it, as a part of the shimmer duration.
    static let staggerRatio: Double = 0.125
    /// The length of one cycle, as a part of the shimmer duration.
    static let periodRatio: Double = 0.75
    /// The shimmer duration in seconds.
    static let shimmer: TimeInterval = TimeInterval(MonarchTokens.Duration.shimmer) / 1000
    /// The length of one cycle in seconds.
    static let period: TimeInterval = shimmer * periodRatio
    /// The delay between one dot and the next, in seconds.
    static let stagger: TimeInterval = shimmer * staggerRatio

    /// The diameter of a dot before Dynamic Type is applied.
    static func dotSize(_ size: MonarchAiTypingDotsSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1
        case .md: MonarchTokens.Space.space1_5
        }
    }

    /// How far through its rise a dot is at a time, from 0 (at rest) to 1
    /// (at the peak). Each dot starts one stagger after the dot before it,
    /// rises for `riseFraction` of the cycle, falls for the same time and
    /// rests for the remainder.
    static func lift(at time: TimeInterval, index: Int) -> Double {
        var local = (time - Double(index) * stagger).truncatingRemainder(dividingBy: period)
        if local < 0 { local += period }
        let phase = local / period
        if phase < riseFraction { return eased(phase / riseFraction) }
        if phase < riseFraction * 2 { return eased(1 - (phase - riseFraction) / riseFraction) }
        return 0
    }

    /// The opacity of a dot for a lift, or the still opacity when `lift` is
    /// `nil` (Reduce Motion).
    static func opacity(lift: Double?) -> Double {
        guard let lift else { return stillOpacity }
        return restingOpacity + (1 - restingOpacity) * min(max(lift, 0), 1)
    }

    /// A smooth step from 0 to 1 with no speed at either end.
    private static func eased(_ value: Double) -> Double {
        let clamped = min(max(value, 0), 1)
        return clamped * clamped * (3 - 2 * clamped)
    }
}
