import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiMentionContext``.
enum MonarchAiMentionContextTokens {
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// Padding before and after a row and a heading.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below a row.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The size of a person's avatar and of the kind icon's frame.
    static let avatarSize: MonarchAvatarSize = .sm
    /// The rows shown before the list scrolls; a half row hints that it scrolls.
    static let visibleRows: CGFloat = 7.5
    /// The height of a row before Dynamic Type, for the scrolling height.
    static let rowHeight: CGFloat = MonarchTokens.Space.space12
    /// The placeholder rows while searching.
    static let placeholderRows = 3
    /// The background of the strip.
    static var background: MonarchColorRole { \.bgSurface }
    /// The line above the strip.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The colour of a kind icon.
    static var icon: MonarchColorRole { \.aiStrong }

    /// The colours of a row's label, description and tokens.
    static func textColors(isDisabled: Bool) -> (label: MonarchColorRole, detail: MonarchColorRole) {
        if isDisabled {
            let disabled: MonarchColorRole = \.textDisabled
            return (disabled, disabled)
        }
        let label: MonarchColorRole = \.textDefault
        let detail: MonarchColorRole = \.textMuted
        return (label, detail)
    }

    /// Whether the menu is shown: while a query is open, except after a query
    /// with a space found nothing, as ``MonarchMentionPicker`` does.
    static func isOpen(query: MonarchMentionQuery?, status: MonarchMentionPickerStatus, itemCount: Int) -> Bool {
        MonarchMentionSuggestions.isOpen(query: query, status: status, itemCount: itemCount)
    }

    /// The results grouped by kind in the order documents, people, records, apps.
    static func groups(_ items: [MonarchAiMentionItem]) -> [(kind: MonarchAiMentionKind, items: [MonarchAiMentionItem])]
    {
        MonarchAiMentionKind.allCases.compactMap { kind in
            let matching = items.filter { $0.kind == kind }
            return matching.isEmpty ? nil : (kind, matching)
        }
    }

    /// The heading of a group, or `nil` when there is only one group.
    static func heading(_ kind: MonarchAiMentionKind, groupCount: Int, labels: MonarchAiMentionContextLabels) -> String?
    {
        guard groupCount > 1 else { return nil }
        return labels.group?(kind) ?? MonarchAiMentionContextStrings.group(kind)
    }

    /// The SF Symbol of a kind, the same as its context chip.
    static func symbol(_ kind: MonarchAiMentionKind) -> String {
        MonarchAiContextFormat.symbol(kind.contextKind)
    }

    /// A result's token estimate: "1.2K tokens".
    static func tokensText(_ count: Int, locale: Locale, labels: MonarchAiMentionContextLabels) -> String {
        let formatted = MonarchAiContextFormat.formatTokens(count, locale: locale)
        return labels.tokens?(formatted, count) ?? MonarchAiContextFormat.tokens(formatted, count: count)
    }

    /// The text when nothing matches: "Type to search" for an empty query, else "Nothing matches “q”".
    static func emptyText(query: String, labels: MonarchAiMentionContextLabels) -> String {
        if let empty = labels.empty { return empty(query) }
        return query.isEmpty
            ? MonarchAiMentionContextStrings.typeToSearch : MonarchAiMentionContextStrings.noMatch(query)
    }

    /// What VoiceOver reads for a row: the label, then the kind, the
    /// description and the token estimate.
    static func rowLabel(_ item: MonarchAiMentionItem, locale: Locale, labels: MonarchAiMentionContextLabels) -> String
    {
        var parts = [item.label]
        if let description = item.description, !description.isEmpty { parts.append(description) }
        if let tokens = item.tokens { parts.append(tokensText(tokens, locale: locale, labels: labels)) }
        return parts.joined(separator: ", ")
    }

    /// What is announced for a state of the search.
    static func announcement(
        status: MonarchMentionPickerStatus,
        count: Int,
        query: String,
        labels: MonarchAiMentionContextLabels
    ) -> String {
        switch status {
        case .loading: labels.loading ?? MonarchAiMentionContextStrings.loading
        case .failed: labels.error ?? MonarchAiMentionContextStrings.error
        case .ready:
            count == 0
                ? emptyText(query: query, labels: labels)
                : labels.count?(count) ?? MonarchAiMentionContextStrings.count(count)
        }
    }

    /// Whether the list scrolls in a fixed height.
    static func scrolls(itemCount: Int) -> Bool {
        CGFloat(itemCount) > visibleRows
    }
}
