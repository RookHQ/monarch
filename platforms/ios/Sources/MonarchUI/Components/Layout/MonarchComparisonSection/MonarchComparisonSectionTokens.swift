import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchComparisonSection``.
enum MonarchComparisonSectionTokens {
    /// The gap between the header, the table, the cards, the note and the actions.
    static let spacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between best-for cards.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding inside a best-for card and the note.
    static let cardPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between a card's title and its description.
    static let cardTextSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The fill of the note.
    static var noteFill: MonarchColorRole { \.bgSubtle }
    /// The colour of the note and card descriptions.
    static var supportingColor: MonarchColorRole { \.textMuted }

    /// The fill of a best-for card.
    static func cardFill(isHighlighted: Bool) -> MonarchColorRole {
        isHighlighted ? \.accentSubtle : \.bgSurface
    }

    /// The border of a best-for card.
    static func cardBorder(isHighlighted: Bool) -> MonarchColorRole {
        isHighlighted ? \.accentBorder : \.borderSubtle
    }

    /// The narrowest the table gets before it scrolls sideways: the feature column and a readable column per option.
    static func tableWidth(columns: Int) -> CGFloat {
        MonarchPricingTableTokens.featureColumnMinWidth + CGFloat(columns)
            * MonarchPricingTableTokens.planColumnMinWidth
    }

    /// Whether the best-for cards sit side by side: two at most, below the accessibility text sizes.
    static func placesCardsInRow(count: Int, isAccessibilitySize: Bool) -> Bool {
        count <= 2 && !isAccessibilitySize
    }
}
