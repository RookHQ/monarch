import Foundation
import MonarchTokens

/// Component tokens for ``MonarchAiResponseSkeleton``.
enum MonarchAiResponseSkeletonTokens {
    /// The gap between lines, cards and rows.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the bars inside a card.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the shape groups (cards, table rows).
    static let groupSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding inside a card and a table row.
    static let cardPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between table cells.
    static let cellSpacing: CGFloat = MonarchTokens.Space.space3
    /// The height of one bar at the default text size; scaled with Dynamic Type.
    static let barHeight: CGFloat = MonarchTokens.Space.space3
    /// The fill of a bar.
    static var bar: MonarchColorRole { \.bgMuted }
    /// The fill of a title or header bar.
    static var strongBar: MonarchColorRole { \.bgEmphasis }
    /// The tint that pulses over the bars.
    static var shimmer: MonarchColorRole { \.aiSubtle }
    /// The fill of a card and of the table.
    static var surface: MonarchColorRole { \.bgSurface }
    /// The fill of the table header row.
    static var headerSurface: MonarchColorRole { \.bgSubtle }
    /// The border of a card and of the table.
    static var border: MonarchColorRole { \.borderSubtle }

    /// The widths of text lines as fractions of the width, repeated in turn.
    /// Fixed, so the placeholder looks the same every time.
    static let widths: [Double] = [1, 0.94, 0.97, 0.88, 0.91, 0.96, 0.85]
    /// The width of the last line when there is more than one.
    static let lastLineWidth: Double = 0.62
    /// The width of a card's title bar.
    static let cardTitleWidth: Double = 0.55
    /// The width of a card's second line.
    static let cardLineWidth: Double = 0.78
    /// The width of a header cell's bar.
    static let headerWidth: Double = 0.6

    /// The number of lines, cards or body rows when none is given.
    static func defaultCount(_ shape: MonarchAiResponseSkeletonShape) -> Int {
        switch shape {
        case .text, .cards: 3
        case .table: 4
        }
    }

    /// The count to draw: the given one or the default, at least 1.
    static func count(_ lines: Int?, shape: MonarchAiResponseSkeletonShape) -> Int {
        max(1, lines ?? defaultCount(shape))
    }

    /// The number of table columns to draw, at least 1.
    static func columns(_ columns: Int) -> Int {
        max(1, columns)
    }

    /// The width of text line `index` out of `count`: the last of several is short.
    static func lineWidth(index: Int, count: Int) -> Double {
        if count > 1 && index == count - 1 { return lastLineWidth }
        return widths[index % widths.count]
    }

    /// The width of the bar in body cell (`row`, `column`).
    static func cellWidth(row: Int, column: Int) -> Double {
        widths[(row + column) % widths.count]
    }

}
