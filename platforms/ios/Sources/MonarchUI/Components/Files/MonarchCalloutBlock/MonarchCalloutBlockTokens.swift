import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchCalloutBlock``.
enum MonarchCalloutBlockTokens {
    /// Padding before and after the content.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the content.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon and the content.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the views of the content.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The SF Symbol of the neutral tone: a plain dot, as in the web block.
    static let neutralSymbol = "circle.fill"

    /// The colour roles of a callout block.
    struct Appearance: Equatable {
        let background: MonarchColorRole
        let icon: MonarchColorRole
        let border: MonarchColorRole
        let text: MonarchColorRole
    }

    /// The roles for a tone, from the shared tone palette. The neutral block
    /// sits on the subtle background.
    static func appearance(tone: MonarchTone) -> Appearance {
        let palette = MonarchTonePalette.palette(for: tone)
        return Appearance(
            background: tone == .neutral ? \.bgSubtle : palette.subtle,
            icon: palette.strong,
            border: palette.border,
            text: \.textDefault
        )
    }

    /// The SF Symbol: the app's, or the one of the tone.
    static func symbol(tone: MonarchTone, override: String?) -> String {
        if let override, !override.isEmpty { return override }
        return tone == .neutral ? neutralSymbol : MonarchTonePalette.palette(for: tone).symbol
    }

    /// The size of the icon: the neutral dot is small.
    static func iconSize(tone: MonarchTone, override: String?) -> MonarchIconSize {
        symbol(tone: tone, override: override) == neutralSymbol ? .sm : .md
    }
}
