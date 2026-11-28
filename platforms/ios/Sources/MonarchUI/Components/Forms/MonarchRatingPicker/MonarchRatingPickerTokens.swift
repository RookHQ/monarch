import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchRatingPicker``.
enum MonarchRatingPickerTokens {
    /// The gap between the label and the scale.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the number and the label of a tile.
    static let tileTextSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the stars and the caption.
    static let captionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of a tile.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The least width of a tile that shows a label under its number.
    static let labelledTileWidth: CGFloat = MonarchTokens.Space.space24
    /// The SF Symbol of a star up to the chosen value.
    static let filledStarSymbol = "star.fill"
    /// The SF Symbol of a star above the chosen value.
    static let emptyStarSymbol = "star"

    /// The gap between tiles.
    static func tileSpacing(_ size: MonarchRatingPickerSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1_5
        case .md: MonarchTokens.Space.space2
        }
    }

    /// Padding before and after the content of a tile.
    static func tileHorizontalPadding(_ size: MonarchRatingPickerSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space2
        case .md: MonarchTokens.Space.space3
        }
    }

    /// Padding above and below the content of a tile.
    static func tileVerticalPadding(_ size: MonarchRatingPickerSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1
        case .md: MonarchTokens.Space.space2
        }
    }

    /// The point size of a star before Dynamic Type is applied.
    static func starSize(_ size: MonarchRatingPickerSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space5
        case .md: MonarchTokens.Space.space6
        }
    }

    /// The least width of a tile: the touch target, or wider when tiles carry labels.
    static func tileMinWidth(hasLabels: Bool) -> CGFloat {
        hasLabels ? labelledTileWidth : MonarchAccessibility.minimumHitTarget
    }

    /// The whole numbers of a range as options without labels.
    static func options(in range: ClosedRange<Int>) -> [MonarchRatingOption] {
        range.map { MonarchRatingOption($0) }
    }

    /// The value after a tile or star is activated: its value, or `nil` when
    /// the chosen one is activated again and the rating can be cleared.
    static func activated(_ value: Int, current: Int?, isClearable: Bool) -> Int? {
        if value == current { return isClearable ? nil : current }
        return value
    }

    /// The value one step up the scale: the first option when nothing is
    /// chosen, and the last option at the end.
    static func incremented(_ current: Int?, options: [MonarchRatingOption]) -> Int? {
        guard let index = options.firstIndex(where: { $0.value == current }) else { return options.first?.value }
        return options[min(index + 1, options.count - 1)].value
    }

    /// The value one step down the scale. Nothing chosen stays nothing, and
    /// the first option stays the first.
    static func decremented(_ current: Int?, options: [MonarchRatingOption]) -> Int? {
        guard let index = options.firstIndex(where: { $0.value == current }) else { return current }
        return options[max(index - 1, 0)].value
    }

    /// Whether a star is filled: every star up to the chosen value.
    static func isFilled(_ value: Int, current: Int?) -> Bool {
        guard let current else { return false }
        return value <= current
    }

    /// What VoiceOver reads as the value: "3 of 5", "3 of 5, Good" or "No rating".
    static func accessibilityValue(_ current: Int?, options: [MonarchRatingOption]) -> String {
        guard let selected = options.first(where: { $0.value == current }), let maximum = options.last?.value else {
            return MonarchRatingPickerStrings.none
        }
        if let label = selected.label, !label.isEmpty {
            return MonarchRatingPickerStrings.value(selected.value, of: maximum, label: label)
        }
        return MonarchRatingPickerStrings.value(selected.value, of: maximum)
    }

    /// The colour roles of a tile in one state.
    static func tileAppearance(isSelected: Bool, isPressed: Bool, isEnabled: Bool) -> MonarchSurfaceAppearance {
        guard isEnabled else {
            return MonarchSurfaceAppearance(background: \.bgSubtle, foreground: \.textDisabled, border: \.borderSubtle)
        }
        if isSelected {
            return MonarchSurfaceAppearance(
                background: isPressed ? \.accentSubtleHover : \.accentSubtle,
                foreground: \.textDefault,
                border: \.accentBorder
            )
        }
        return MonarchSurfaceAppearance(
            background: isPressed ? \.bgEmphasis : \.bgSurface,
            foreground: \.textMuted,
            border: isPressed ? \.borderStrong : \.borderDefault
        )
    }

    /// The width of the border of a tile. The chosen tile has a heavier border.
    static func tileBorderWidth(isSelected: Bool) -> CGFloat {
        isSelected ? MonarchTokens.Space.space0_5 : MonarchTokens.Space.px
    }

    /// The role of a star.
    static func starForeground(isFilled: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.textDisabled }
        return isFilled ? \.warningStrong : \.borderStrong
    }
}
