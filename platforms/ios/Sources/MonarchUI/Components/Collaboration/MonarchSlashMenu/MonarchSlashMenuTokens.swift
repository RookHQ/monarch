import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchSlashMenu``.
enum MonarchSlashMenuTokens {
    /// The gap between the icon and the text of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// Padding before and after the content of a row.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the content of a row.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The number of rows shown before the list scrolls; half a row more is
    /// shown so it is clear that it scrolls.
    static let visibleRows: CGFloat = 4.5
    /// The height of one row before Dynamic Type, for the scrolling limit.
    static let rowHeight: CGFloat = MonarchTokens.Space.space12
    /// The side of the square that frames a row's icon.
    static let iconTile: CGFloat = MonarchTokens.Space.space6 + MonarchTokens.Space.space1
    /// The colours of the panel.
    static var background: MonarchColorRole { \.bgSurface }
    static var border: MonarchColorRole { \.borderSubtle }
    static var heading: MonarchColorRole { \.textMuted }

    /// The colours of a row's icon, label and description.
    static func colors(
        isDisabled: Bool
    ) -> (icon: MonarchColorRole, label: MonarchColorRole, description: MonarchColorRole) {
        let disabled: MonarchColorRole = \.textDisabled
        if isDisabled { return (disabled, disabled, disabled) }
        let icon: MonarchColorRole = \.textMuted
        let label: MonarchColorRole = \.textDefault
        return (icon, label, icon)
    }

    /// Filters items by the query: labels that start with it first, then
    /// labels that contain it, then matches in the id, the description or
    /// the keywords. Case and diacritics are ignored; an empty query keeps
    /// every item. Ties keep the given order.
    static func filter(_ items: [MonarchSlashMenuItem], query: String) -> [MonarchSlashMenuItem] {
        let needle = fold(query.trimmingCharacters(in: .whitespaces))
        guard !needle.isEmpty else { return items }
        let ranked = items.enumerated().compactMap {
            index,
            item -> (rank: Int, index: Int, item: MonarchSlashMenuItem)? in
            let label = fold(item.label)
            let rest = fold(([item.id, item.description ?? ""] + item.keywords).joined(separator: " "))
            let rank =
                label.hasPrefix(needle) ? 0 : label.contains(needle) ? 1 : rest.contains(needle) ? 2 : -1
            return rank < 0 ? nil : (rank, index, item)
        }
        return ranked.sorted { ($0.rank, $0.index) < ($1.rank, $1.index) }.map(\.item)
    }

    /// Splits items into groups in display order: the groups named in
    /// `order` first, then any others in the order they first appear.
    static func groups(
        _ items: [MonarchSlashMenuItem],
        order: [String]
    ) -> [(heading: String?, items: [MonarchSlashMenuItem])] {
        var keys: [String?] = order.map { Optional($0) }
        var buckets: [String?: [MonarchSlashMenuItem]] = [:]
        for item in items {
            if !keys.contains(item.group) { keys.append(item.group) }
            buckets[item.group, default: []].append(item)
        }
        return keys.compactMap { key in
            guard let list = buckets[key], !list.isEmpty else { return nil }
            return (key, list)
        }
    }

    /// The items in the order they are shown, which is the order of the groups.
    static func ordered(_ items: [MonarchSlashMenuItem], order: [String]) -> [MonarchSlashMenuItem] {
        groups(items, order: order).flatMap(\.items)
    }

    /// What VoiceOver reads for a row: the label, then the description.
    static func accessibilityLabel(_ item: MonarchSlashMenuItem) -> String {
        guard let description = item.description, !description.isEmpty else { return item.label }
        return "\(item.label), \(description)"
    }

    /// The text shown when nothing matches.
    static func emptyText(_ query: MonarchSlashMenuQuery, labels: MonarchSlashMenuLabels) -> String {
        let typed = "\(query.trigger)\(query.text)"
        return labels.empty?(typed) ?? MonarchSlashMenuStrings.empty(typed)
    }

    /// What is announced when the list changes.
    static func announcement(
        count: Int,
        query: MonarchSlashMenuQuery,
        labels: MonarchSlashMenuLabels
    ) -> String {
        count == 0
            ? emptyText(query, labels: labels)
            : labels.count?(count) ?? MonarchSlashMenuStrings.count(count)
    }

    /// Whether the list scrolls in a fixed height.
    static func scrolls(itemCount: Int) -> Bool {
        CGFloat(itemCount) > visibleRows
    }

    private static func fold(_ text: String) -> String {
        text.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: nil)
    }
}
