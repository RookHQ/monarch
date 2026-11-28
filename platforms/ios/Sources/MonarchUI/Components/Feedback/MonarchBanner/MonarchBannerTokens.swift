import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchBanner``.
enum MonarchBannerTokens {
    /// Padding before and after the content.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the content.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the message.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the text and the action.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The corner radius.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px

    /// The colour roles of a banner.
    struct Appearance: Equatable {
        let background: MonarchColorRole
        let icon: MonarchColorRole
        let border: MonarchColorRole
    }

    /// The roles for a tone, from the shared tone palette. The neutral banner
    /// sits on the subtle background, one step lighter than a neutral badge.
    static func appearance(tone: MonarchTone) -> Appearance {
        let palette = MonarchTonePalette.palette(for: tone)
        return Appearance(
            background: tone == .neutral ? \.bgSubtle : palette.subtle,
            icon: palette.strong,
            border: palette.border
        )
    }

    /// The weight of the title: semibold, as the web alert title.
    static let titleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)

    /// The colour of the message: muted under a title, so the title leads;
    /// the default text colour when the message stands alone.
    static func messageColor(hasTitle: Bool) -> MonarchColorRole {
        hasTitle ? \.textMuted : \.textDefault
    }

    /// What VoiceOver reads for the banner: the tone name, the title and the
    /// message, so the meaning never depends on the colour or the icon.
    static func spokenLabel(tone: String?, title: String?, message: String?) -> String {
        MonarchSpokenLabel.join([tone, title, message])
    }

    /// The SF Symbol for a tone: the web alert's default glyph for it (platforms/icons.json). The web shows the
    /// warning triangle for danger too, and the info circle for neutral.
    static func symbol(tone: MonarchTone) -> String {
        switch tone {
        case .neutral, .info: "info.circle"
        case .success: "checkmark.circle"
        case .warning, .danger: "exclamationmark.triangle"
        default: MonarchTonePalette.palette(for: tone).symbol
        }
    }

    /// The banner's own SF Symbol when it has one, else the tone's.
    static func symbol(tone: MonarchTone, override: String?) -> String {
        if let override, !override.isEmpty { return override }
        return symbol(tone: tone)
    }
}
