import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and row rules for ``MonarchBarList``.
enum MonarchBarListTokens {
    /// The gap between two rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the rank, the label and the value.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between an icon and its text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// Padding before and after the label, inside the bar.
    static let labelHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below the label, inside the bar.
    static let labelVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the label line and the bar, and the bar's height and the rank column width.
    static let trackSpacing: CGFloat = MonarchTokens.Space.space1_5
    static let trackHeight: CGFloat = 6
    static let rankWidth: CGFloat = 24
    /// The least height of a row's bar.
    static let minRowHeight: CGFloat = MonarchTokens.Control.sm
    /// Shown in place of a value that is missing.
    static let missingSymbol = "–"

    /// The weight of the label and value. A highlighted row is heavier, so
    /// the emphasis is not colour alone.
    static func labelWeight(isHighlighted: Bool) -> Font.Weight {
        MonarchTextStyleSpec.fontWeight(
            isHighlighted ? MonarchTokens.FontWeight.semibold : MonarchTokens.FontWeight.medium
        )
    }

    /// The fill of a row's bar.
    static func bar(isHighlighted: Bool) -> MonarchColorRole {
        \.data1
    }

    /// One row to draw.
    struct Row: Identifiable, Equatable {
        /// The item.
        let item: MonarchBarListItem
        /// The length of the bar, 0 through 1.
        let fraction: Double
        /// The position in the list, from 1.
        let rank: Int

        var id: String { item.id }
    }

    /// The rows to draw and how many there are before `limit`.
    struct Layout: Equatable {
        /// The rows shown.
        let rows: [Row]
        /// The number of items.
        let total: Int
    }

    /// Sorts, limits and scales the items. Pure, so it is tested without a view.
    ///
    /// Items without a value, or with a value that is not finite, go last and
    /// have no bar. Equal values keep their given order. The scale is
    /// `maximum`, or the largest value of all items, not only of those shown.
    static func layout(items: [MonarchBarListItem], sort: MonarchBarListSort, maximum: Double?, limit: Int?) -> Layout {
        func finite(_ item: MonarchBarListItem) -> Double? {
            item.value.flatMap { $0.isFinite ? $0 : nil }
        }
        var indexed = Array(items.enumerated())
        if sort != .none {
            indexed.sort { first, second in
                switch (finite(first.element), finite(second.element)) {
                case (nil, nil): return first.offset < second.offset
                case (nil, _): return false
                case (_, nil): return true
                case (let lhs?, let rhs?):
                    if lhs == rhs { return first.offset < second.offset }
                    return sort == .descending ? lhs > rhs : lhs < rhs
                }
            }
        }
        let scale = maximum ?? max(0, items.compactMap(finite).max() ?? 0)
        let shown = limit.map { Array(indexed.prefix(max($0, 0))) } ?? indexed
        let rows = shown.enumerated().map { position, entry in
            let fraction: Double =
                if let value = finite(entry.element), scale > 0 { min(max(value / scale, 0), 1) } else { 0 }
            return Row(item: entry.element, fraction: fraction, rank: position + 1)
        }
        return Layout(rows: rows, total: items.count)
    }

    /// The text drawn, redacted, in a placeholder row.
    static let placeholderName = "Placeholder label"

    /// Placeholder items for the loading state, with bars of falling length.
    /// At least one row, at most twelve.
    static func placeholderItems(count: Int) -> [MonarchBarListItem] {
        let rows = min(max(count, 1), 12)
        return (0..<rows).map { index in
            MonarchBarListItem(placeholderName, id: "placeholder-\(index)", value: Double(rows - index))
        }
    }

    /// The visible value of an item, or `nil` when it has none.
    static func valueText(for item: MonarchBarListItem, locale: Locale, formatValue: ((Double) -> String)?) -> String? {
        if let displayValue = item.displayValue { return displayValue }
        guard let value = item.value, value.isFinite else { return nil }
        if let formatValue { return formatValue(value) }
        return value.formatted(.number.locale(locale))
    }

    /// What VoiceOver reads as the name of a row: the label, then the description.
    static func accessibilityLabel(for item: MonarchBarListItem) -> String {
        guard let description = item.description, !description.isEmpty else { return item.label }
        return "\(item.label), \(description)"
    }
}
