import Foundation
import MonarchTokens

/// Component tokens and logic for ``MonarchAiSourceList``. The source
/// helpers (`symbol`, `host`, `safeURL`, `meta`) are shared with
/// ``MonarchAiSourcePreview``.
enum MonarchAiSourceListTokens {
    /// The gap between the heading, the groups and the toggle.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between a group heading and its rows.
    static let groupSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the glyph and the text of a row.
    static let glyphSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the lines of a row.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the number and the title.
    static let numberSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below a row.
    static let rowVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// Padding before and after a row, inside its pressed background.
    static let rowHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of a row's pressed background.
    static let rowCornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The most lines of a snippet.
    static let snippetLineLimit = 2
    /// How many sources show before the list collapses, by default.
    static let defaultCollapseAfter = 3

    /// The heading and group label colour.
    static var headingColor: MonarchColorRole { \.textMuted }
    /// The colour of the `[n]` number.
    static var numberColor: MonarchColorRole { \.aiCitation }
    /// The meta line and snippet colour.
    static var supportingColor: MonarchColorRole { \.textMuted }
    /// The background of a pressed row.
    static var pressedBackground: MonarchColorRole { \.bgSubtle }

    /// The colour of a title: the link colour when it opens something.
    static func titleColor(isActionable: Bool) -> MonarchColorRole {
        \.textDefault
    }

    /// One line under a title: the host, the product and the kind, joined with " · ".
    static func meta(_ source: MonarchAiSourcePart, kindName: String) -> String {
        [MonarchAiSourceFormat.host(source.url), source.product, kindName]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: " · ")
    }

    /// A source with its number in citation order.
    struct Entry: Identifiable, Hashable {
        /// The citation number, from 1.
        let number: Int
        /// The source.
        let item: MonarchAiSourceListItem
        /// The id of the source.
        var id: String { item.id }
    }

    /// What the list shows.
    struct Layout: Hashable {
        /// The used sources on show.
        let used: [Entry]
        /// The considered sources on show.
        let considered: [Entry]
        /// How many sources are used.
        let usedCount: Int
        /// How many sources were considered.
        let consideredCount: Int
        /// Whether the sources are shown in two groups: only when some were considered.
        let isGrouped: Bool
        /// Whether there are more sources than `collapseAfter`, so the toggle shows.
        let isCollapsible: Bool
    }

    /// Numbers the sources in order, splits them into used and considered,
    /// and, when collapsed, keeps the first `collapseAfter` of used then considered.
    ///
    /// - Parameters:
    ///   - items: The sources in citation order.
    ///   - collapseAfter: The number shown while collapsed; `nil` never collapses.
    ///   - isExpanded: Whether the person expanded the list.
    /// - Returns: The sources on show in each group, the totals and whether the list groups and collapses.
    static func layout(
        _ items: [MonarchAiSourceListItem],
        collapseAfter: Int?,
        isExpanded: Bool
    ) -> Layout {
        let entries = items.enumerated().map { Entry(number: $0.offset + 1, item: $0.element) }
        let used = entries.filter(\.item.isUsed)
        let considered = entries.filter { !$0.item.isUsed }
        let limit = collapseAfter.map { max(0, $0) }
        let isCollapsible = limit.map { items.count > $0 } ?? false
        var budget = isCollapsible && !isExpanded ? (limit ?? items.count) : items.count
        let usedShown = Array(used.prefix(budget))
        budget -= usedShown.count
        let consideredShown = Array(considered.prefix(max(0, budget)))
        return Layout(
            used: usedShown,
            considered: consideredShown,
            usedCount: used.count,
            consideredCount: considered.count,
            isGrouped: !considered.isEmpty,
            isCollapsible: isCollapsible
        )
    }

    /// What VoiceOver reads for a row: "Source 1: title", then the meta line and the snippet.
    static func accessibilityLabel(
        number: Int?,
        title: String,
        meta: String,
        snippet: String?,
        sourceLabel: (Int, String) -> String
    ) -> String {
        let name = number.map { sourceLabel($0, title) } ?? title
        return [name, meta, snippet ?? ""].filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
