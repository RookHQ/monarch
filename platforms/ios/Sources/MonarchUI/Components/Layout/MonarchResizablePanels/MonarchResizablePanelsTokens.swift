import MonarchTokens
import SwiftUI

/// Component tokens and the size rules for ``MonarchResizablePanels``.
enum MonarchResizablePanelsTokens {
    /// The thickness of the separator line.
    static let lineThickness: CGFloat = MonarchShapes.borderWidth
    /// The thickness of the separator line while it is dragged or focused.
    static let activeLineThickness: CGFloat = MonarchTokens.Space.space0_5
    /// The diameter of a dot of the grip.
    static let gripDotSize: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The gap between the dots of the grip, and the room around them.
    static let gripDotSpacing: CGFloat = MonarchTokens.Space.space0_5

    /// The colour of the separator line and the grip.
    static func handleColor(isActive: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.borderSubtle }
        return isActive ? \.accentStrong : \.borderDefault
    }

    /// A range that lies inside 0 to 1.
    static func normalized(_ range: ClosedRange<Double>) -> ClosedRange<Double> {
        let lower = min(max(range.lowerBound, 0), 1)
        let upper = min(max(range.upperBound, lower), 1)
        return lower...upper
    }

    /// A fraction limited to a range. A value that is not a number becomes the lower bound.
    static func clamped(_ fraction: Double, to range: ClosedRange<Double>) -> Double {
        guard !fraction.isNaN else { return range.lowerBound }
        return min(max(fraction, range.lowerBound), range.upperBound)
    }

    /// The length of the first panel, with half the separator taken from it.
    static func firstLength(fraction: Double, total: CGFloat) -> CGFloat {
        max(0, total * fraction - lineThickness / 2)
    }

    /// The fraction after a drag.
    ///
    /// - Parameters:
    ///   - start: The fraction when the drag began.
    ///   - translation: How far the finger moved along the axis, in points.
    ///   - total: The length of both panels along the axis, in points.
    ///   - isMirrored: Whether the first panel is at the right, as in a
    ///     horizontal arrangement in a right-to-left layout.
    ///   - range: The allowed fractions.
    /// - Returns: The share of the first panel, inside `range`.
    static func fraction(
        start: Double,
        translation: CGFloat,
        total: CGFloat,
        isMirrored: Bool,
        range: ClosedRange<Double>
    ) -> Double {
        guard total > 0 else { return clamped(start, to: range) }
        let delta = Double(translation / total)
        return clamped(start + (isMirrored ? -delta : delta), to: range)
    }

    /// The fraction after one step from the keyboard or from VoiceOver.
    static func fraction(
        _ fraction: Double,
        applying move: MonarchResizablePanelsMove,
        step: Double,
        range: ClosedRange<Double>
    ) -> Double {
        switch move {
        case .decrease: clamped(fraction - abs(step), to: range)
        case .increase: clamped(fraction + abs(step), to: range)
        case .minimum: range.lowerBound
        case .maximum: range.upperBound
        }
    }

    /// The step a key makes, or `nil` when the key does not move the separator.
    ///
    /// The arrow keys along the axis move it one step, in the direction they
    /// point on screen, so left and right swap in a right-to-left layout.
    /// Home and End move it to the ends of the range.
    static func move(
        for key: KeyEquivalent,
        axis: MonarchResizablePanelsAxis,
        isRightToLeft: Bool
    ) -> MonarchResizablePanelsMove? {
        switch (key, axis) {
        case (.home, _): .minimum
        case (.end, _): .maximum
        case (.leftArrow, .horizontal): isRightToLeft ? .increase : .decrease
        case (.rightArrow, .horizontal): isRightToLeft ? .decrease : .increase
        case (.upArrow, .vertical): .decrease
        case (.downArrow, .vertical): .increase
        default: nil
        }
    }

    /// Whether the panels are stacked at their natural height with no separator to drag.
    static func isStacked(axis: MonarchResizablePanelsAxis, stacksWhenCompact: Bool, isCompact: Bool) -> Bool {
        axis == .horizontal && stacksWhenCompact && isCompact
    }

    /// What VoiceOver reads as the value of the separator: the share of the first panel.
    static func accessibilityValue(fraction: Double) -> String {
        fraction.formatted(.percent.precision(.fractionLength(0)))
    }
}
