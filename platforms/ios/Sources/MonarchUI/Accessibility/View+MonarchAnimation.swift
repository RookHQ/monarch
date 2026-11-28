import SwiftUI

/// Animates a value change unless Reduce Motion is on.
struct MonarchAnimationModifier<Value: Equatable>: ViewModifier {
    let duration: MonarchMotionDuration
    let easing: MonarchMotionEasing
    let value: Value

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content.animation(
            theme.motion.animation(duration, easing: easing, reduceMotion: reduceMotion),
            value: value
        )
    }
}

extension View {
    /// Animates changes of `value` with Monarch's motion tokens.
    ///
    /// When Reduce Motion is on the change is applied without animation.
    public func monarchAnimation<Value: Equatable>(
        _ duration: MonarchMotionDuration = .base,
        easing: MonarchMotionEasing = .standard,
        value: Value
    ) -> some View {
        modifier(MonarchAnimationModifier(duration: duration, easing: easing, value: value))
    }
}
