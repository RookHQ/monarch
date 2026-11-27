import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchPlanCard``.
enum MonarchPlanCardTokens {
    /// The gap between the sections of the card.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between items on one line: the name and badges, the price and period.
    static let inlineSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between lines of text that belong together.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between two features.
    static let featureSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a feature's mark and its text.
    static let markSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the border of a highlighted card. Wider than a normal
    /// border, so the emphasis is not colour alone.
    static let highlightBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The border role of a highlighted card.
    static var highlightBorder: MonarchColorRole { \.accentBorder }
    /// The SF Symbol of an included feature.
    static let includedSymbol = "checkmark"
    /// The SF Symbol of a feature that is not included.
    static let notIncludedSymbol = "minus"

    /// The shadow under the card. A highlighted card is raised.
    static func elevation(isHighlighted: Bool) -> MonarchElevationLevel {
        isHighlighted ? .md : .xs
    }

    /// The variant of the badge beside the name. Always quiet: the card border and shadow carry the emphasis.
    static func badgeVariant(isHighlighted: Bool) -> MonarchBadgeVariant {
        _ = isHighlighted
        return .subtle
    }

    /// The symbol of a feature's mark.
    static func markSymbol(isIncluded: Bool) -> String {
        isIncluded ? includedSymbol : notIncludedSymbol
    }

    /// The colour roles of a feature's mark and text.
    static func featureRoles(isIncluded: Bool) -> (mark: MonarchColorRole, text: MonarchColorRole) {
        if isIncluded {
            return (mark: \.accentStrong, text: \.textDefault)
        }
        return (mark: \.textSubtle, text: \.textMuted)
    }

    /// What VoiceOver reads for a feature: the label and the hint, with the
    /// "not included" prefix when the plan does not have it.
    static func featureAccessibilityLabel(_ feature: MonarchPlanCardFeature, notIncludedLabel: String) -> String {
        var text = feature.label
        if let hint = feature.hint, !hint.isEmpty {
            text += ", \(hint)"
        }
        return feature.isIncluded ? text : "\(notIncludedLabel) \(text)"
    }
}
