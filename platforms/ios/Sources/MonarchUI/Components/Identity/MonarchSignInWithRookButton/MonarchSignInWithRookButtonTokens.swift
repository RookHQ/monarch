import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchSignInWithRookButton``.
enum MonarchSignInWithRookButtonTokens {
    /// The least height of the visible button.
    static func minHeight(_ size: MonarchSignInWithRookButtonSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Control.sm
        case .md: MonarchTokens.Control.lg
        case .lg: MonarchTokens.Control.xl
        }
    }

    /// The side of the mark before Dynamic Type is applied.
    static func markSize(_ size: MonarchSignInWithRookButtonSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space4
        case .md: MonarchTokens.Space.space5
        case .lg: MonarchTokens.Space.space6
        }
    }

    /// The gap between the mark and the title.
    static func spacing(_ size: MonarchSignInWithRookButtonSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space2
        case .md: MonarchTokens.Space.space2_5
        case .lg: MonarchTokens.Space.space3
        }
    }

    /// Padding before the mark.
    static func leadingPadding(_ size: MonarchSignInWithRookButtonSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space2
        case .md, .lg: MonarchTokens.Space.space3
        }
    }

    /// Padding after the title.
    static func trailingPadding(_ size: MonarchSignInWithRookButtonSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space3
        case .md: MonarchTokens.Space.space4
        case .lg: MonarchTokens.Space.space5
        }
    }

    /// The type role of the title.
    static func textStyle(_ size: MonarchSignInWithRookButtonSize) -> MonarchTextStyle {
        switch size {
        case .sm: .caption
        case .md: .label
        case .lg: .body
        }
    }

    /// The weight of the title at every size.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// Padding above and below the title, so a title that wraps keeps a margin.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5

    /// The theme the button takes its colours from. The light, dark and
    /// neutral variants are pinned, so the button looks the same on every
    /// screen; they keep the screen's contrast setting.
    static func theme(for variant: MonarchSignInWithRookButtonVariant, in current: MonarchTheme) -> MonarchTheme {
        switch variant {
        case .light, .neutral: current.isHighContrast ? .highContrastLight : .light
        case .dark: current.isHighContrast ? .highContrastDark : .dark
        case .auto: current
        }
    }

    /// The colour roles for a variant and state, read from the theme of the variant.
    static func appearance(
        variant: MonarchSignInWithRookButtonVariant,
        isPressed: Bool,
        isEnabled: Bool
    ) -> MonarchSurfaceAppearance {
        let isNeutral = variant == .neutral
        guard isEnabled else {
            return MonarchSurfaceAppearance(
                background: isNeutral ? \.bgMuted : \.bgSurface,
                foreground: \.textDisabled,
                border: isNeutral ? nil : \.borderSubtle
            )
        }
        if isNeutral {
            return MonarchSurfaceAppearance(
                background: isPressed ? \.bgEmphasis : \.bgMuted,
                foreground: \.textDefault,
                border: nil
            )
        }
        return MonarchSurfaceAppearance(
            background: isPressed ? \.bgMuted : \.bgSurface,
            foreground: \.textDefault,
            border: \.borderStrong
        )
    }
}
