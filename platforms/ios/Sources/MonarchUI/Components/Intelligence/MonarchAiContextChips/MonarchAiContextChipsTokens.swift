import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiContextChips``.
enum MonarchAiContextChipsTokens {
    /// The gap between chips.
    static let spacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the chips and the total.
    static let totalSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the parts of a chip.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// Padding before the content of a chip, and after it when there is no remove button.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space2_5
    /// Padding above and below the text inside the visible chip.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The least height of the visible chip.
    static let minHeight: CGFloat = MonarchTokens.Control.xs
    /// The widest the name grows before Dynamic Type, then it truncates in the middle: 192 points.
    static let labelMaxWidth: CGFloat = MonarchTokens.Space.space24 * 2
    /// How far the remove button's touch target reaches back over the end of the chip.
    static let removeOverlap: CGFloat = MonarchTokens.Space.space3
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The width of one bar of the weight mark.
    static let barWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the bars of the weight mark.
    static let barSpacing: CGFloat = MonarchTokens.Space.px
    /// The heights of the three bars of the weight mark, shortest first.
    static let barHeights: [CGFloat] = [
        MonarchTokens.Space.space1, MonarchTokens.Space.space1_5, MonarchTokens.Space.space2,
    ]
    /// The SF Symbol of the remove button.
    static let removeSymbol = "xmark"
    /// The weight of the name.
    static let labelWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)

    /// How far the touch target extends above and below the visible chip when it has a remove button.
    static func hitInset(isRemovable: Bool) -> CGFloat {
        guard isRemovable else { return 0 }
        return max(0, (MonarchAccessibility.minimumHitTarget - minHeight) / 2)
    }

    /// The number of filled bars of the weight mark: one, two or three.
    static func filledBars(_ weight: MonarchAiContextWeight) -> Int {
        switch weight {
        case .light: 1
        case .medium: 2
        case .heavy: 3
        }
    }

    /// The colour role of one bar of the weight mark.
    static func barRole(index: Int, weight: MonarchAiContextWeight) -> MonarchColorRole {
        index < filledBars(weight) ? \.aiStrong : \.borderDefault
    }

    /// The colour role of the token count: heavy items are drawn in the default text colour.
    static func tokensRole(_ weight: MonarchAiContextWeight) -> MonarchColorRole {
        weight == .heavy ? \.textDefault : \.textMuted
    }

    /// The colour roles of the chip: AI subtle fill and AI border; muted when disabled.
    static func appearance(isEnabled: Bool) -> MonarchSurfaceAppearance {
        isEnabled
            ? MonarchSurfaceAppearance(background: \.aiSubtle, foreground: \.textDefault, border: \.aiBorder)
            : MonarchSurfaceAppearance(background: \.bgMuted, foreground: \.textDisabled, border: \.borderSubtle)
    }

    /// The sum of the known token counts.
    static func total(_ items: [MonarchAiContextItem]) -> Int {
        items.reduce(0) { $0 + max(0, $1.tokens ?? 0) }
    }

    /// Whether the total is shown: by default when any item has a token count, and never with no items.
    static func showsTotal(_ items: [MonarchAiContextItem], override: Bool?) -> Bool {
        guard !items.isEmpty else { return false }
        return override ?? items.contains { $0.tokens != nil }
    }

    /// The weight of an item against the budget, or against the total without one. `nil` without a count.
    static func weight(_ item: MonarchAiContextItem, total: Int, budget: Int?) -> MonarchAiContextWeight? {
        guard let tokens = item.tokens else { return nil }
        return MonarchAiContextWeight(tokens: tokens, reference: budget ?? total)
    }

    /// The token count text of an item.
    static func tokensText(_ count: Int, labels: MonarchAiContextChipsLabels, locale: Locale) -> String {
        let formatted = MonarchAiContextFormat.formatTokens(count, locale: locale)
        return labels.tokens?(formatted, count) ?? MonarchAiContextFormat.tokens(formatted, count: count)
    }

    /// The total text.
    static func totalText(_ count: Int, labels: MonarchAiContextChipsLabels, locale: Locale) -> String {
        let formatted = MonarchAiContextFormat.formatTokens(count, locale: locale)
        return labels.total?(formatted, count) ?? MonarchAiContextChipsStrings.total(formatted, count: count)
    }

    /// The spoken prefix of a kind.
    static func kindName(_ kind: MonarchAiContextKind, labels: MonarchAiContextChipsLabels) -> String {
        labels.kind?(kind) ?? MonarchAiContextChipsStrings.kind(kind)
    }

    /// What VoiceOver reads for a chip: "File: contract.pdf, 1.2K tokens".
    static func accessibilityLabel(kind: String, label: String, tokens: String?) -> String {
        let named = "\(kind): \(label)"
        guard let tokens else { return named }
        return "\(named), \(tokens)"
    }
}
