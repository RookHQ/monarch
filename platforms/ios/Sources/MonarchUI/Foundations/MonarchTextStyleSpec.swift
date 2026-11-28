import MonarchTokens
import SwiftUI

/// The token values behind one type role.
struct MonarchTextStyleSpec {
    /// The size in points before Dynamic Type is applied.
    let size: CGFloat
    /// A token weight, 100 to 900.
    let weight: Int
    /// Line height as a multiple of the size.
    let lineHeight: CGFloat
    /// Tracking in em.
    let letterSpacing: CGFloat
    /// The system text style whose Dynamic Type curve the role follows.
    let relativeTo: Font.TextStyle
    /// Whether the role uses a monospaced face.
    let isMonospaced: Bool

    /// The line height of the system font as a multiple of its size. SwiftUI
    /// takes extra spacing between lines, not a line height, so the modifier
    /// adds only the part of the token line height above this value.
    static let systemLineHeight: CGFloat = MonarchTokens.LineHeight.tight

    /// The SwiftUI weight for a token weight.
    static func fontWeight(_ weight: Int) -> Font.Weight {
        switch weight {
        case ..<450: .regular
        case ..<550: .medium
        case ..<650: .semibold
        default: .bold
        }
    }
}

extension MonarchTextStyle {
    /// The token values for this role.
    var spec: MonarchTextStyleSpec {
        typealias Size = MonarchTokens.FontSize
        typealias Weight = MonarchTokens.FontWeight
        typealias Leading = MonarchTokens.LineHeight
        typealias Tracking = MonarchTokens.LetterSpacing
        func make(
            _ size: Double,
            _ weight: Int,
            _ lineHeight: Double,
            _ letterSpacing: Double,
            _ relativeTo: Font.TextStyle,
            monospaced: Bool = false
        ) -> MonarchTextStyleSpec {
            MonarchTextStyleSpec(
                size: size,
                weight: weight,
                lineHeight: lineHeight,
                letterSpacing: letterSpacing,
                relativeTo: relativeTo,
                isMonospaced: monospaced
            )
        }
        return switch self {
        case .display: make(Size.xl4, Weight.bold, Leading.tight, Tracking.tight, .largeTitle)
        case .title: make(Size.xl2, Weight.semibold, Leading.tight, Tracking.snug, .title)
        case .heading: make(Size.xl, Weight.semibold, Leading.snug, Tracking.snug, .title2)
        case .subheading: make(Size.lg, Weight.semibold, Leading.snug, Tracking.normal, .headline)
        case .body: make(Size.base, Weight.regular, Leading.normal, Tracking.normal, .body)
        case .bodyStrong: make(Size.base, Weight.semibold, Leading.normal, Tracking.normal, .body)
        case .label: make(Size.sm, Weight.medium, Leading.snug, Tracking.normal, .subheadline)
        case .caption: make(Size.xs, Weight.regular, Leading.snug, Tracking.normal, .caption)
        case .code: make(Size.sm, Weight.regular, Leading.normal, Tracking.normal, .body, monospaced: true)
        case .codeSmall:
            make(Size.xs, Weight.regular, Leading.normal, Tracking.normal, .caption, monospaced: true)
        case .bodySmall: make(Size.sm, Weight.regular, Leading.normal, Tracking.normal, .subheadline)
        case .bodyExtraSmall: make(Size.xs, Weight.regular, Leading.normal, Tracking.normal, .caption)
        }
    }
}
