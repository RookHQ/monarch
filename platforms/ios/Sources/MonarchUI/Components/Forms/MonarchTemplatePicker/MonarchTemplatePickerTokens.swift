import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchTemplatePicker``.
enum MonarchTemplatePickerTokens {
    /// The gap between the filters and the grid.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between cards.
    static let gridSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between category filters.
    static let filterSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding inside a card.
    static let cardPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the preview and the text of a card.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the name and the description.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The narrowest a card gets before the grid drops a column, before Dynamic Type: 144 points.
    static let minCardWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space12
    /// The corner radius of a card.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The corner radius of the preview.
    static let previewRadius: CGFloat = MonarchTokens.Radius.md
    /// The width-to-height ratio of the preview.
    static let previewAspectRatio: CGFloat = 16.0 / 10.0
    /// How far the check mark sits from the card's corner.
    static let checkInset: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the blank card.
    static let blankSymbol = "plus"
    /// The SF Symbol of a template without a preview.
    static let placeholderSymbol = "doc.text"
    /// The SF Symbol on the selected card.
    static let checkSymbol = "checkmark.circle.fill"

    /// Whether a template matches a search: every word of the query appears in
    /// the name, the description or the keywords, in any order, ignoring case
    /// and diacritics.
    static func matches(_ item: MonarchTemplatePickerItem, query: String) -> Bool {
        let words = query.split(whereSeparator: \.isWhitespace).map(String.init)
        guard !words.isEmpty else { return true }
        let haystack = ([item.name, item.description ?? ""] + item.keywords).joined(separator: " ")
        return words.allSatisfy { haystack.range(of: $0, options: [.caseInsensitive, .diacriticInsensitive]) != nil }
    }

    /// The templates shown for a category (`nil` for all) and a query, in order.
    static func filtered(
        _ templates: [MonarchTemplatePickerItem],
        category: String?,
        query: String
    ) -> [MonarchTemplatePickerItem] {
        templates.filter { (category == nil || $0.category == category) && matches($0, query: query) }
    }

    /// Whether a filter is narrowing the templates.
    static func isFiltering(category: String?, query: String) -> Bool {
        category != nil || !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// What VoiceOver reads after a template's name: the badge, then the description.
    static func accessibilityValue(badge: String?, description: String?) -> String {
        [badge, description].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The colour roles of a card, shared with the radio cards so choices look alike.
    static func appearance(isSelected: Bool, isPressed: Bool, isEnabled: Bool) -> MonarchRadioCardGroupTokens.Appearance
    {
        MonarchRadioCardGroupTokens.appearance(isSelected: isSelected, isPressed: isPressed, isEnabled: isEnabled)
    }

    /// The border width of a card: thicker when selected, so selection is not shown by colour alone.
    static func borderWidth(isSelected: Bool) -> CGFloat {
        MonarchRadioCardGroupTokens.borderWidth(isSelected: isSelected)
    }
}
