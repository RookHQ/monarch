import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiFollowUpSuggestions``.
enum MonarchAiFollowUpSuggestionsTokens {
    /// The gap between chips.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the text of a chip.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the text of a chip.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The least height of the visible chip.
    static let minHeight: CGFloat = MonarchTokens.Control.sm
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The most lines of one suggestion; a longer one is truncated at the end.
    static let lineLimit = 2
    /// The widest a chip grows before Dynamic Type, so one long suggestion does not fill the row: 288 points.
    static let maxChipWidth: CGFloat = MonarchTokens.Space.space24 * 3
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .label

    /// How far the touch target extends above and below the visible chip, so it is 44 points high.
    static var hitInset: CGFloat {
        max(0, (MonarchAccessibility.minimumHitTarget - minHeight) / 2)
    }

    /// The colour roles of one chip.
    struct Appearance: Equatable {
        /// The fill.
        let background: MonarchColorRole
        /// The border.
        let border: MonarchColorRole
        /// The text.
        let foreground: MonarchColorRole
    }

    /// The roles for a state: a bordered surface that takes the AI tint while pressed. Pure.
    static func appearance(isPressed: Bool, isEnabled: Bool) -> Appearance {
        guard isEnabled else {
            return Appearance(background: \.bgMuted, border: \.borderSubtle, foreground: \.textDisabled)
        }
        return Appearance(
            background: isPressed ? \.aiSubtle : \.bgSurface,
            border: \.aiBorder,
            foreground: \.aiStrong
        )
    }

    /// The suggestions that are shown: trimmed, without empty ones or repeats, in order.
    static func visible(_ suggestions: [String]) -> [String] {
        var seen = Set<String>()
        var result: [String] = []
        for suggestion in suggestions {
            let trimmed = suggestion.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty, seen.insert(trimmed).inserted else { continue }
            result.append(trimmed)
        }
        return result
    }
}
