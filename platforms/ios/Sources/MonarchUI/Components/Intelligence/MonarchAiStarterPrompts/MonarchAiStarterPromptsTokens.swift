import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiStarterPrompts``.
enum MonarchAiStarterPromptsTokens {
    /// The least width of a card in the grid before Dynamic Type is applied: 208 points.
    static let columnMinWidth: CGFloat = MonarchTokens.Space.space24 * 2 + MonarchTokens.Space.space4
    /// Padding before and after the content of a starter.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title row and the prompt.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the icon and the title.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The corner radius, the same as a card.
    static let cornerRadius: CGFloat = MonarchCardTokens.cornerRadius
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The most lines of the prompt that are shown.
    static let promptLineLimit = 2

    /// The gap between starters.
    static func spacing(_ layout: MonarchAiStarterPromptsLayout) -> CGFloat {
        switch layout {
        case .grid: MonarchTokens.Space.space2
        case .list: MonarchTokens.Space.space1_5
        }
    }

    /// Padding above and below the content of a starter.
    static func verticalPadding(_ layout: MonarchAiStarterPromptsLayout) -> CGFloat {
        switch layout {
        case .grid: MonarchTokens.Space.space3
        case .list: MonarchTokens.Space.space2
        }
    }

    /// Whether the starters are in one column: always for the list, and for
    /// the grid at the accessibility text sizes, where two columns cannot fit.
    static func isSingleColumn(layout: MonarchAiStarterPromptsLayout, isAccessibilitySize: Bool) -> Bool {
        layout == .list || isAccessibilitySize
    }

    /// The colour roles of one starter.
    struct Appearance: Equatable {
        /// The fill.
        let background: MonarchColorRole
        /// The border.
        let border: MonarchColorRole
        /// The title.
        let title: MonarchColorRole
        /// The icon.
        let icon: MonarchColorRole
        /// The prompt text.
        let prompt: MonarchColorRole
    }

    /// The roles for a state. Pure, so it is tested without a view.
    static func appearance(isPressed: Bool, isEnabled: Bool) -> Appearance {
        guard isEnabled else {
            return Appearance(
                background: \.bgSurface,
                border: \.borderSubtle,
                title: \.textDisabled,
                icon: \.textDisabled,
                prompt: \.textDisabled
            )
        }
        return Appearance(
            background: isPressed ? \.aiSubtle : \.bgSurface,
            border: isPressed ? \.aiBorder : \.borderDefault,
            title: \.textDefault,
            icon: \.aiStrong,
            prompt: \.textMuted
        )
    }

    /// What VoiceOver reads after the title: the prompt, unless it is hidden.
    static func accessibilityHint(prompt: String, hidesPrompt: Bool) -> String {
        hidesPrompt ? "" : prompt
    }
}
