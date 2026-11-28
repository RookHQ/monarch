import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchRadioCardGroup``.
enum MonarchRadioCardGroupTokens {
    /// The gap between the label and the cards.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between cards.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space3
    /// Padding before and after the content of a card.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the content of a card.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon, the text and the selection mark.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title row and the description.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the title and the meta text.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of a card.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The diameter of the selection mark before Dynamic Type is applied.
    static let markSize: CGFloat = MonarchTokens.Space.space5
    /// The diameter of the dot inside the selection mark, as a part of the mark.
    static let markDotRatio: CGFloat = 0.5
    /// The most columns a group can have.
    static let maximumColumns = 3
    /// The SF Symbol before a detail line.
    static let detailSymbol = "checkmark"

    /// The width of the border of a card. The selected card has a heavier border.
    static func borderWidth(isSelected: Bool) -> CGFloat {
        isSelected ? MonarchTokens.Space.space0_5 : MonarchTokens.Space.px
    }

    /// The number of columns to lay out: one on a compact width and at the
    /// accessibility text sizes, otherwise the requested number from 1 to 3.
    static func columnCount(requested: Int, isCompactWidth: Bool, isAccessibilitySize: Bool) -> Int {
        if isCompactWidth || isAccessibilitySize { return 1 }
        return min(max(requested, 1), maximumColumns)
    }

    /// The colour roles of a card in one state.
    struct Appearance: Equatable {
        let background: MonarchColorRole
        let border: MonarchColorRole
        let title: MonarchColorRole
        let secondary: MonarchColorRole
        let icon: MonarchColorRole
        let mark: MonarchColorRole
    }

    /// The roles for a state. Pure, so it is tested without a view.
    static func appearance(isSelected: Bool, isPressed: Bool, isEnabled: Bool) -> Appearance {
        guard isEnabled else {
            return Appearance(
                background: \.bgSubtle,
                border: \.borderSubtle,
                title: \.textDisabled,
                secondary: \.textDisabled,
                icon: \.textDisabled,
                mark: \.textDisabled
            )
        }
        if isSelected {
            return Appearance(
                background: isPressed ? \.bgEmphasis : \.accentSubtle,
                border: \.accentStrong,
                title: \.textDefault,
                secondary: \.textMuted,
                icon: \.accentStrong,
                mark: \.accentStrong
            )
        }
        return Appearance(
            background: isPressed ? \.bgEmphasis : \.bgSurface,
            border: isPressed ? \.borderStrong : \.borderDefault,
            title: \.textDefault,
            secondary: \.textMuted,
            icon: \.textMuted,
            mark: \.borderInput
        )
    }

    /// What VoiceOver reads after the title: the meta text, the description,
    /// then each detail line.
    static func accessibilityValue(meta: String?, description: String?, details: [String] = []) -> String {
        ([meta, description].compactMap { $0 } + details).filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
