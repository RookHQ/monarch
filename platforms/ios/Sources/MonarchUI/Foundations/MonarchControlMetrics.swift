import MonarchTokens
import SwiftUI

/// The measured sizes of Monarch's own controls, in one place, so a style
/// never writes a raw number. The Android library has the same table
/// (`MonarchControlMetrics`).
///
/// Web sizes come from `MonarchTokens.Control` and the component CSS. Native
/// differs on purpose where a thumb needs more room: fields are 40, 44 and
/// 52 pt, and every control keeps a 44 pt hit area around a smaller visible
/// shape. Sizes are minimums: text uses `monarchTextStyle`, so a control grows
/// with Dynamic Type instead of clipping.
enum MonarchControlMetrics {
    // Fields.

    /// The minimum visible height of a field of `size`: 40, 44 and 52 pt.
    static func fieldMinHeight(_ size: MonarchFieldSize) -> CGFloat {
        switch size {
        case .small: MonarchTokens.Control.lg
        case .medium: max(MonarchTokens.Control.lg, MonarchAccessibility.minimumHitTarget)
        case .large: MonarchTokens.Space.space12 + MonarchTokens.Space.space1
        }
    }

    /// Horizontal padding inside a field: 10, 12 and 16 pt.
    static func fieldHorizontalPadding(_ size: MonarchFieldSize) -> CGFloat {
        switch size {
        case .small: MonarchTokens.Space.space2_5
        case .medium: MonarchTokens.Space.space3
        case .large: MonarchTokens.Space.space4
        }
    }

    /// The minimum height of a multi-line field: 72 pt.
    static let textAreaMinHeight: CGFloat = MonarchTokens.Control.md * 2

    /// The width of the border of a field and of every other bordered control.
    static let borderWidth: CGFloat = MonarchTokens.Space.px

    /// The width of the focus ring around a focused field or control: 2 pt.
    static let focusRingWidth: CGFloat = MonarchTokens.Space.space0_5

    /// The gap between a control's edge and its focus ring: 2 pt. The web
    /// draws a field's ring flush; native uses the offset of buttons and
    /// checks for every control (D11, PREMIUM.md rule 10).
    static let focusRingOffset: CGFloat = MonarchTokens.Space.space0_5

    /// The size of an icon inside a field, button or menu row.
    static let iconSize: CGFloat = MonarchTokens.Space.space4

    // Selection controls.

    /// The visible box of a checkbox and the ring of a radio: 16 pt.
    static let checkSize: CGFloat = MonarchTokens.Space.space4

    /// The dot of a selected radio: 10 pt.
    static let radioDotSize: CGFloat = MonarchTokens.Space.space2_5

    // Feedback.

    /// The height of a progress bar: 4 pt small, 8 pt default.
    static let progressBarSmall: CGFloat = MonarchTokens.Space.space1
    static let progressBarMedium: CGFloat = MonarchTokens.Space.space2

    /// The diameter of a spinner: 14, 16 and 24 pt, with a 2 pt stroke.
    static let spinnerSmall: CGFloat = 14
    static let spinnerMedium: CGFloat = MonarchTokens.Space.space4
    static let spinnerLarge: CGFloat = MonarchTokens.Space.space6
    static let spinnerStroke: CGFloat = MonarchTokens.Space.space0_5

    // Navigation.

    /// The visible height of a tab (36 pt) and the thickness of its indicator (2 pt).
    static let tabHeight: CGFloat = MonarchTokens.Control.md
    static let tabIndicator: CGFloat = MonarchTokens.Space.space0_5
}
