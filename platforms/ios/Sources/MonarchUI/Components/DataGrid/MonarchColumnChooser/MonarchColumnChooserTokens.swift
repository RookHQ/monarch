import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchColumnChooser``.
enum MonarchColumnChooserTokens {
    /// The gap between the search icon, the field and the clear button, and
    /// between a column's name and its note.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The colour of the count, the notes and the search icon.
    static var mutedRole: MonarchColorRole { \.textMuted }
    /// The colour of a column's name.
    static var labelRole: MonarchColorRole { \.textDefault }
    /// The search field symbol.
    static let searchSymbol = "magnifyingglass"
    /// The clear-search symbol.
    static let clearSymbol = "xmark.circle.fill"

    /// The columns in display order: those in `order` first, in its order,
    /// then the rest in their given order. Unknown ids in `order` are skipped.
    static func ordered(_ columns: [MonarchColumnChooserColumn], order: [String]?) -> [MonarchColumnChooserColumn] {
        guard let order, !order.isEmpty else { return columns }
        let position = Dictionary(order.enumerated().map { ($1, $0) }, uniquingKeysWith: { first, _ in first })
        return columns.enumerated()
            .sorted { lhs, rhs in
                let left = position[lhs.element.id] ?? order.count + lhs.offset
                let right = position[rhs.element.id] ?? order.count + rhs.offset
                return left == right ? lhs.offset < rhs.offset : left < right
            }
            .map(\.element)
    }

    /// Whether a column is shown: always when it cannot be hidden.
    static func isShown(_ column: MonarchColumnChooserColumn, hidden: Set<String>) -> Bool {
        !column.isHideable || !hidden.contains(column.id)
    }

    /// The number of columns shown.
    static func shownCount(_ columns: [MonarchColumnChooserColumn], hidden: Set<String>) -> Int {
        columns.filter { isShown($0, hidden: hidden) }.count
    }

    /// Whether the search field is offered.
    static func isSearchable(columnCount: Int, threshold: Int) -> Bool {
        columnCount >= threshold
    }

    /// The columns whose name contains the query, ignoring case and
    /// diacritics. An empty query, or one when search is off, keeps them all.
    static func filtered(
        _ columns: [MonarchColumnChooserColumn],
        query: String,
        isSearchable: Bool
    ) -> [MonarchColumnChooserColumn] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard isSearchable, !trimmed.isEmpty else { return columns }
        return columns.filter { $0.label.localizedStandardContains(trimmed) }
    }

    /// Whether a search is narrowing the list, which turns reordering off.
    static func isFiltering(query: String, isSearchable: Bool) -> Bool {
        isSearchable && !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// Whether a column's switch is disabled: it cannot be hidden, or it is
    /// shown and hiding it would go under the minimum.
    static func isToggleDisabled(
        _ column: MonarchColumnChooserColumn,
        hidden: Set<String>,
        shownCount: Int,
        minimumVisible: Int
    ) -> Bool {
        if !column.isHideable { return true }
        return isShown(column, hidden: hidden) && shownCount <= minimumVisible
    }

    /// The hidden set after a switch changes.
    static func setting(_ id: String, isShown: Bool, in hidden: Set<String>) -> Set<String> {
        var result = hidden
        if isShown {
            result.remove(id)
        } else {
            result.insert(id)
        }
        return result
    }

    /// The order after moving the column `id` by `step` places, or `nil` when
    /// it would leave the list.
    static func moving(_ id: String, by step: Int, in ids: [String]) -> [String]? {
        guard let from = ids.firstIndex(of: id) else { return nil }
        let to = from + step
        guard ids.indices.contains(to), step != 0 else { return nil }
        var result = ids
        result.remove(at: from)
        result.insert(id, at: to)
        return result
    }

    /// The order after a drag in the list, with SwiftUI's `onMove` offsets.
    static func moving(fromOffsets source: IndexSet, toOffset destination: Int, in ids: [String]) -> [String] {
        var result = ids
        result.move(fromOffsets: source, toOffset: destination)
        return result
    }

    /// The count under the title.
    static func countText(shown: Int, total: Int, labels: MonarchColumnChooserLabels) -> String {
        labels.shownCount?(shown, total) ?? MonarchColumnChooserStrings.shownCount(shown, total)
    }

    /// The announcement after a move, the position from 1.
    static func movedAnnouncement(_ column: String, to index: Int, count: Int, labels: MonarchColumnChooserLabels)
        -> String
    {
        labels.moved?(column, index + 1, count) ?? MonarchColumnChooserStrings.moved(column, index + 1, count)
    }

    /// Whether Show all is offered: some column is hidden.
    static func showsShowAll(shown: Int, total: Int) -> Bool {
        shown < total
    }
}
