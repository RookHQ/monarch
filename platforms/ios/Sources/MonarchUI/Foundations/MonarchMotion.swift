import SwiftUI

/// Builds animations from the motion tokens.
///
/// Read it from the theme: `theme.motion`. In a view, prefer
/// `monarchAnimation(_:easing:value:)`, which reads the
/// Reduce Motion setting for you.
public struct MonarchMotion: Sendable, Hashable {
    /// Motion from the generated tokens.
    public init() {}

    /// An animation, or `nil` when the change should be immediate.
    ///
    /// - Parameters:
    ///   - duration: How long the transition takes.
    ///   - easing: The curve. `.standard` is the system `.snappy` spring at
    ///     the duration; the others are the token's cubic Bézier curves.
    ///   - reduceMotion: Pass `accessibilityReduceMotion` from the
    ///     environment. When `true` the result is `nil`.
    /// - Returns: The animation, or `nil` for an immediate change.
    public func animation(
        _ duration: MonarchMotionDuration = .base,
        easing: MonarchMotionEasing = .standard,
        reduceMotion: Bool
    ) -> Animation? {
        guard !reduceMotion, duration != .instant else { return nil }
        // A state change (a switch, a selected segment, a chevron) uses the
        // platform's snappy spring, critically damped so nothing bounces, at
        // the token's duration. Entrances and exits keep the token curves.
        if easing == .standard { return .snappy(duration: duration.seconds) }
        let curve = easing.curve
        return .timingCurve(curve.x1, curve.y1, curve.x2, curve.y2, duration: duration.seconds)
    }
}
