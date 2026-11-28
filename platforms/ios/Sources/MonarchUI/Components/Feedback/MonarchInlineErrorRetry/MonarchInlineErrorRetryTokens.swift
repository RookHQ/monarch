import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchInlineErrorRetry``.
enum MonarchInlineErrorRetryTokens {
    /// The gap between the icon and the message.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the message and the button.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the content of the banner variant.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the content of the banner variant.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The SF Symbol of the retry button.
    static let retrySymbol = "arrow.clockwise"

    /// The colour roles of the row.
    struct Appearance: Equatable {
        /// The fill, or `nil` for no fill.
        let background: MonarchColorRole?
        /// The border, or `nil` for no border.
        let border: MonarchColorRole?
        /// The icon.
        let icon: MonarchColorRole
        /// The message.
        let text: MonarchColorRole
    }

    /// The roles for a tone and variant. Only the banner has a surface.
    static func appearance(tone: MonarchTone, variant: MonarchInlineErrorRetryVariant) -> Appearance {
        let palette = MonarchTonePalette.palette(for: tone)
        switch variant {
        case .inline:
            return Appearance(background: nil, border: nil, icon: palette.strong, text: \.textDefault)
        case .banner:
            return Appearance(
                background: tone == .neutral ? \.bgSubtle : palette.subtle,
                border: palette.border,
                icon: palette.strong,
                text: \.textDefault
            )
        }
    }

    /// The SF Symbol: the app's, or the one of the tone.
    static func symbol(tone: MonarchTone, override: String?) -> String {
        if let override, !override.isEmpty { return override }
        // The web row draws its warning triangle for the default danger tone.
        if tone == .danger { return "exclamationmark.triangle" }
        return MonarchTonePalette.palette(for: tone).symbol
    }

    /// Whether a change of the busy state is announced: only when a retry starts.
    static func announcesRetry(wasRetrying: Bool, isRetrying: Bool) -> Bool {
        !wasRetrying && isRetrying
    }
}
