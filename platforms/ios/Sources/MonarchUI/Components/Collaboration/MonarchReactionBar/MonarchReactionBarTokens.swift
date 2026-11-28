import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchReactionBar``.
enum MonarchReactionBarTokens {
    /// The least height of the visible chip.
    static func minHeight(_ size: MonarchReactionBarSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space6
        case .md: MonarchTokens.Control.xs
        }
    }

    /// Padding before and after the content of a chip.
    static func horizontalPadding(_ size: MonarchReactionBarSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space2
        case .md: MonarchTokens.Space.space2_5
        }
    }

    /// The type role of the count.
    static func textStyle(_ size: MonarchReactionBarSize) -> MonarchTextStyle {
        switch size {
        case .sm: .caption
        case .md: .label
        }
    }

    /// How far the touch target reaches above and below the visible chip.
    static func hitInset(_ size: MonarchReactionBarSize, isInteractive: Bool) -> CGFloat {
        guard isInteractive else { return 0 }
        return max(0, (MonarchAccessibility.minimumHitTarget - minHeight(size)) / 2)
    }

    /// The gap between chips in a row.
    static let spacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between rows of a read-only bar. Interactive chips carry their own touch margin.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the emoji and the count.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below the content of a chip.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The SF Symbol of the add button.
    static let addSymbol = "face.smiling"

    /// The colour roles and border width of a chip.
    struct Appearance: Equatable {
        let surface: MonarchSurfaceAppearance
        let borderWidth: CGFloat
        let isCountBold: Bool
    }

    /// The colours of a chip. A reacted chip takes the accent, a thicker
    /// border and a bold count, so the state does not depend on colour alone.
    static func appearance(isReacted: Bool, isEnabled: Bool) -> Appearance {
        guard isEnabled else {
            return Appearance(
                surface: MonarchSurfaceAppearance(
                    background: \.bgMuted,
                    foreground: \.textDisabled,
                    border: \.borderSubtle
                ),
                borderWidth: MonarchShapes.borderWidth,
                isCountBold: isReacted
            )
        }
        if isReacted {
            return Appearance(
                surface: MonarchSurfaceAppearance(
                    background: \.accentSubtle,
                    foreground: \.accentStrong,
                    border: \.accentBorder
                ),
                borderWidth: MonarchTokens.Space.space0_5,
                isCountBold: true
            )
        }
        return Appearance(
            surface: MonarchSurfaceAppearance(
                background: \.bgSurface,
                foreground: \.textDefault,
                border: \.borderSubtle
            ),
            borderWidth: MonarchShapes.borderWidth,
            isCountBold: false
        )
    }

    /// The reactions that are shown: those with a count above zero, each
    /// emoji once.
    static func shown(_ reactions: [MonarchReactionBarItem]) -> [MonarchReactionBarItem] {
        var seen = Set<String>()
        return reactions.filter { $0.count > 0 && seen.insert($0.emoji).inserted }
    }

    /// Whether the add button is shown: the bar is interactive, adding is
    /// allowed and the limit of distinct reactions is not reached.
    static func canAdd(isInteractive: Bool, isAddable: Bool, shownCount: Int, maxReactions: Int?) -> Bool {
        guard isInteractive, isAddable else { return false }
        guard let maxReactions else { return true }
        return shownCount < maxReactions
    }

    /// What a pick from the emoji picker does: react with it, unless the
    /// person already reacted with that emoji.
    static func reactionToAdd(_ emoji: String, in reactions: [MonarchReactionBarItem]) -> String? {
        let existing = shown(reactions).first { $0.emoji == emoji }
        return existing?.isReacted == true ? nil : emoji
    }

    /// The spoken name of an emoji: the item's label, the name in the
    /// built-in emoji index, or the emoji itself.
    static func name(_ item: MonarchReactionBarItem) -> String {
        if let label = item.label, !label.isEmpty { return label }
        return builtInName(item.emoji) ?? item.emoji
    }

    /// The name of an emoji in the emoji picker's built-in index.
    static func builtInName(_ emoji: String) -> String? {
        for category in MonarchEmojiPickerCategory.defaults {
            if let item = category.emojis.first(where: { $0.emoji == emoji }) { return item.name }
        }
        return nil
    }

    /// The spoken name of a chip: the emoji's name and the count.
    static func chipLabel(_ item: MonarchReactionBarItem, labels: MonarchReactionBarLabels) -> String {
        let name = name(item)
        return labels.reaction?(name, item.count) ?? MonarchReactionBarStrings.reaction(name, item.count)
    }
}
