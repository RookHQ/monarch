import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchTag``.
enum MonarchTagTokens {
    /// The least height of the visible tag.
    static func minHeight(_ size: MonarchTagSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space6
        case .md: MonarchTokens.Control.xs
        }
    }

    /// Padding before and after the content.
    static func horizontalPadding(_ size: MonarchTagSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space2
        case .md: MonarchTokens.Space.space2_5
        }
    }

    /// The type role of the text.
    static func textStyle(_ size: MonarchTagSize) -> MonarchTextStyle {
        switch size {
        case .sm: .caption
        case .md: .label
        }
    }

    /// How far the touch target of an interactive tag extends above and below
    /// the visible tag, so the target is 44 points high while the tag is not.
    static func hitInset(_ size: MonarchTagSize, isInteractive: Bool) -> CGFloat {
        guard isInteractive else { return 0 }
        return max(0, (MonarchAccessibility.minimumHitTarget - minHeight(size)) / 2)
    }

    /// Padding above and below the text inside the visible tag.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the icon, the selection mark and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// How far the touch target of the remove button reaches back over the
    /// end of the label, so the glyph sits close to the text.
    static let removeOverlap: CGFloat = MonarchTokens.Space.space3
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The weight of the text.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The SF Symbol of the remove button.
    static let removeSymbol = "xmark"
    /// The SF Symbol that marks a selected tag.
    static let selectedSymbol = "checkmark"

    /// The colour roles of a tag in one state. A selected tag takes the
    /// accent whatever its tone; a disabled tag is muted.
    static func appearance(tone: MonarchTone, isSelected: Bool, isEnabled: Bool) -> MonarchSurfaceAppearance {
        guard isEnabled else {
            return MonarchSurfaceAppearance(background: \.bgMuted, foreground: \.textDisabled, border: \.borderSubtle)
        }
        if isSelected {
            return MonarchSurfaceAppearance(
                background: \.accentSubtle,
                foreground: \.accentStrong,
                border: \.accentBorder
            )
        }
        let palette = MonarchTonePalette.palette(for: tone)
        return MonarchSurfaceAppearance(
            background: palette.subtle,
            foreground: tone == .neutral ? \.textDefault : palette.strong,
            border: palette.border
        )
    }

    /// What VoiceOver reads for the remove button.
    static func removeLabel(text: String, override: String?) -> String {
        if let override, !override.isEmpty { return override }
        return MonarchTagStrings.remove(text)
    }
}
