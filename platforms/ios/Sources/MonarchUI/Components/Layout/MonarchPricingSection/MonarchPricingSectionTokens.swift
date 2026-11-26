import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchPricingSection``.
enum MonarchPricingSectionTokens {
    /// The gap between the header, the picker, the plans, the note and the comparison.
    static let spacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between plan cards.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space4
    /// The extra gap above the comparison, on top of `spacing`.
    static let comparisonSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the comparison title and its table.
    static let comparisonTitleSpacing: CGFloat = MonarchTokens.Space.space4
    /// The colour of the note.
    static var noteColor: MonarchColorRole { \.textMuted }

    /// Whether the period picker is shown: only with two or more periods.
    static func showsPicker(periods: Int) -> Bool { periods > 1 }

    /// The selected period: the bound one, else the stored one, else the first, if it is one of the periods.
    static func selectedPeriod(bound: String?, stored: String?, periods: [MonarchPricingPeriod]) -> String? {
        MonarchPricingTableTokens.selectedPeriod(bound: bound, stored: stored, periods: periods)
    }

    /// Whether the plan cards sit side by side: at regular width, below the accessibility text sizes, for up to
    /// four plans.
    static func placesCardsInRow(isRegularWidth: Bool, isAccessibilitySize: Bool, plans: Int) -> Bool {
        isRegularWidth && !isAccessibilitySize && plans <= 4
    }
}
