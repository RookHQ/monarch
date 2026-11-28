import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchCalloutBanner``.
enum MonarchCalloutBannerTokens {
    /// Padding before and after the content.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4

    /// Padding above and below the content.
    static func verticalPadding(_ variant: MonarchCalloutBannerVariant) -> CGFloat {
        switch variant {
        case .callout: MonarchTokens.Space.space3
        case .scope: MonarchTokens.Space.space2
        }
    }

    /// The gap between the icon and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the message.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the text and the action.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// How far the touch target of the dismiss button reaches into the
    /// padding of the banner, so the glyph lines up with the content.
    static let dismissOutset: CGFloat = MonarchTokens.Space.space3
    /// How far the dismiss button reaches into the padding above and below
    /// the content: never further than that padding.
    static func dismissVerticalOutset(_ variant: MonarchCalloutBannerVariant) -> CGFloat {
        min(dismissOutset, verticalPadding(variant))
    }

    /// The SF Symbol of the dismiss button.
    static let dismissSymbol = "xmark"
    /// The SF Symbol of the scope variant.
    static let scopeSymbol = "line.3.horizontal.decrease.circle"

    /// The type role of the message.
    static func messageStyle(_ variant: MonarchCalloutBannerVariant) -> MonarchTextStyle {
        switch variant {
        case .callout: .label
        case .scope: .caption
        }
    }

    /// The weight of the title: semibold, as the web alert title.
    static let titleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)

    /// The colour of the message: muted under a title, so the title leads;
    /// the default text colour when the message stands alone.
    static func messageColor(hasTitle: Bool) -> MonarchColorRole {
        hasTitle ? \.textMuted : \.textDefault
    }

    /// The colour roles of a banner.
    struct Appearance: Equatable {
        let background: MonarchColorRole
        let icon: MonarchColorRole
        let border: MonarchColorRole
    }

    /// The roles for a tone and variant. The scope variant is neutral
    /// whatever the tone. A neutral callout sits on the subtle background.
    static func appearance(tone: MonarchTone, variant: MonarchCalloutBannerVariant) -> Appearance {
        let effective = effectiveTone(tone, variant: variant)
        let palette = MonarchTonePalette.palette(for: effective)
        return Appearance(
            background: effective == .neutral ? \.bgSubtle : palette.subtle,
            icon: palette.strong,
            border: palette.border
        )
    }

    /// The tone that is drawn and spoken: neutral for the scope variant.
    static func effectiveTone(_ tone: MonarchTone, variant: MonarchCalloutBannerVariant) -> MonarchTone {
        variant == .scope ? .neutral : tone
    }

    /// The SF Symbol: the app's, or the one of the variant and tone.
    static func symbol(tone: MonarchTone, variant: MonarchCalloutBannerVariant, override: String?) -> String {
        if let override, !override.isEmpty { return override }
        if variant == .scope { return scopeSymbol }
        // The web draws its info glyph (a circled i) for the neutral, info and accent tones of a banner.
        if tone == .neutral || tone == .accent { return "info.circle" }
        return MonarchTonePalette.palette(for: tone).symbol
    }

    /// The spoken name of the tone, read before the text, or `nil` when
    /// there is nothing to say (the neutral tone and the scope variant).
    static func spokenTone(tone: MonarchTone, variant: MonarchCalloutBannerVariant, override: String?) -> String? {
        if let override, !override.isEmpty { return override }
        return MonarchToneStrings.name(effectiveTone(tone, variant: variant))
    }
}
