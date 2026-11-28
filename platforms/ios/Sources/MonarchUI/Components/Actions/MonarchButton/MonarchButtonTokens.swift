import MonarchTokens
import SwiftUI

/// Component tokens for the button.
enum MonarchButtonTokens {
    /// The least height of the visible button.
    static func minHeight(_ size: MonarchButtonSize) -> CGFloat {
        switch size {
        case .xs: MonarchTokens.Control.xs
        case .sm: MonarchTokens.Control.sm
        case .md: MonarchTokens.Control.md
        case .lg: MonarchTokens.Control.lg
        }
    }

    /// Padding before and after the label.
    static func horizontalPadding(_ size: MonarchButtonSize) -> CGFloat {
        switch size {
        case .xs: MonarchTokens.Space.space2
        case .sm: MonarchTokens.Space.space2_5
        case .md: MonarchTokens.Space.space3
        case .lg: MonarchTokens.Space.space4
        }
    }

    /// Padding before and after the label. An icon-only button keeps the
    /// vertical padding on every side, so it is square.
    static func horizontalPadding(_ size: MonarchButtonSize, isIconOnly: Bool) -> CGFloat {
        isIconOnly ? verticalPadding : horizontalPadding(size)
    }

    /// Whether the button is drawn as one icon: only when asked and when
    /// there is a symbol to show. Without a symbol the title is shown.
    static func showsIconOnly(isIconOnly: Bool, systemImage: String?) -> Bool {
        isIconOnly && systemImage?.isEmpty == false
    }

    /// The type role of the label.
    static func textStyle(_ size: MonarchButtonSize) -> MonarchTextStyle {
        switch size {
        case .xs, .sm: .caption
        case .md, .lg: .label
        }
    }

    /// The type role of the label of a button of `variant`. The small ghost
    /// button has no fill or outline, so its label is the only affordance: it
    /// keeps the 14 pt label role, so a row action reads as a button next to
    /// body text and not as a caption.
    static func textStyle(_ size: MonarchButtonSize, variant: MonarchButtonVariant) -> MonarchTextStyle {
        if size == .sm, variant == .ghost { return .label }
        return textStyle(size)
    }

    /// The weight of the label at every size.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// Padding above and below the label, so a label that wraps keeps a margin.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between an icon and the label.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The corner radius.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
}
