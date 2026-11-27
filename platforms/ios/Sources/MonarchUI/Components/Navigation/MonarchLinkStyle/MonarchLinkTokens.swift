import MonarchTokens
import SwiftUI

/// How a link looks in one state.
struct MonarchLinkAppearance: Equatable {
    /// The colour of the title and the icon.
    let foreground: MonarchColorRole
    /// Whether the title is underlined.
    let isUnderlined: Bool
}

/// Component tokens for ``MonarchLinkStyle``.
enum MonarchLinkTokens {
    /// The gap between the title and the external icon.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol after the title of a link that leaves the app.
    static let externalSymbol = "arrow.up.right.square"

    /// The weight a variant sets, or `nil` to keep the weight of the surrounding text.
    static func fontWeight(_ variant: MonarchLinkVariant) -> Font.Weight? {
        switch variant {
        case .inline, .subtle: nil
        case .standalone: MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
        }
    }

    /// The colour and underline for a variant and state.
    ///
    /// An inline link is always underlined. The other variants gain the
    /// underline while pressed and when the Button Shapes setting is on, so a
    /// link is not told from text by colour alone for people who ask for that.
    static func appearance(
        variant: MonarchLinkVariant,
        isPressed: Bool,
        isEnabled: Bool,
        showsButtonShapes: Bool
    ) -> MonarchLinkAppearance {
        let isUnderlined = variant == .inline || showsButtonShapes || (isPressed && isEnabled)
        guard isEnabled else {
            return MonarchLinkAppearance(foreground: \.textDisabled, isUnderlined: isUnderlined)
        }
        let foreground: MonarchColorRole =
            switch variant {
            case .inline, .standalone: \.textLink
            case .subtle: isPressed ? \.textDefault : \.textMuted
            }
        return MonarchLinkAppearance(foreground: foreground, isUnderlined: isUnderlined)
    }
}
