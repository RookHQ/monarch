import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchDashboardGrid``.
enum MonarchDashboardGridTokens {
    /// The gap between widgets, in both directions.
    static let spacing: CGFloat = MonarchTokens.Space.space4
    /// The gap inside the arrange bar.
    static let barSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding of the arrange bar.
    static let barPadding: CGFloat = MonarchTokens.Space.space1
    /// The grid width below which there is one column (the web's 40rem).
    static let twoColumnWidth: CGFloat = MonarchTokens.Breakpoint.sm
    /// The grid width from which the grid has `columns` columns (the web's 64rem).
    static let fullColumnWidth: CGFloat = MonarchTokens.Breakpoint.lg
    /// The most columns a grid can have.
    static let maximumColumns = 4
    /// The fill of the arrange bar.
    static var barFill: MonarchColorRole { \.bgSubtle }
    /// The colour of the bar's label and grip.
    static var barText: MonarchColorRole { \.textMuted }
    /// The outline of the widget a drag is over.
    static var dropTargetRole: MonarchColorRole { \.accentStrong }
    /// The width of that outline: twice the hairline, so it is seen in every theme.
    static let dropTargetWidth: CGFloat = MonarchShapes.borderWidth * 2
    /// The grip symbol of the arrange bar.
    static let gripSymbol = "line.3.horizontal"

    /// The order of ids: the stored layout's known ids first, without
    /// repeats, then the items missing from it in their own order. A stored
    /// layout survives widgets being added or removed.
    static func resolve<ID: Hashable>(ids: [ID], layout: [ID]) -> [ID] {
        let known = Set(ids)
        var seen = Set<ID>()
        var result: [ID] = []
        for id in layout where known.contains(id) && !seen.contains(id) {
            seen.insert(id)
            result.append(id)
        }
        for id in ids where !seen.contains(id) {
            seen.insert(id)
            result.append(id)
        }
        return result
    }

    /// The number of columns: one below the two-column width or at the
    /// accessibility text sizes, at most two below the full width, then
    /// `columns` (1 to 4).
    static func columnCount(width: CGFloat, columns: Int, isAccessibilitySize: Bool) -> Int {
        let wanted = min(max(1, columns), maximumColumns)
        if isAccessibilitySize || width < twoColumnWidth { return 1 }
        if width < fullColumnWidth { return min(2, wanted) }
        return wanted
    }

    /// The columns a widget spans: clamped to 1 through the column count.
    static func span(_ span: MonarchDashboardGridSpan, columns: Int) -> Int {
        switch span {
        case .full: max(1, columns)
        case .columns(let count): min(max(1, count), max(1, columns))
        }
    }

    /// Packs widgets into rows in order: a widget that does not fit in what
    /// is left of a row starts the next one. Returns the indices of each row.
    static func rows(spans: [Int], columns: Int) -> [[Int]] {
        var rows: [[Int]] = []
        var current: [Int] = []
        var used = 0
        for (index, span) in spans.enumerated() {
            if used + span > columns, !current.isEmpty {
                rows.append(current)
                current = []
                used = 0
            }
            current.append(index)
            used += span
        }
        if !current.isEmpty { rows.append(current) }
        return rows
    }

    /// The width of a widget spanning `span` of `columns` in a grid `width` wide.
    static func width(span: Int, columns: Int, total width: CGFloat, spacing: CGFloat) -> CGFloat {
        let count = CGFloat(max(1, columns))
        let column = max(0, (width - spacing * (count - 1)) / count)
        return column * CGFloat(span) + spacing * CGFloat(max(0, span - 1))
    }

    /// The index after moving one place earlier (-1) or later (+1), or `nil` at an end.
    static func target(from index: Int, step: Int, count: Int) -> Int? {
        let target = index + step
        return (0..<count).contains(target) && step != 0 ? target : nil
    }

    /// The symbols of the earlier and later buttons: up and down in one
    /// column, backward and forward otherwise (mirrored right to left).
    static func moveSymbols(columns: Int) -> (earlier: String, later: String) {
        columns <= 1 ? ("chevron.up", "chevron.down") : ("chevron.backward", "chevron.forward")
    }

    /// The label of the earlier button.
    static func moveEarlierLabel(_ widget: String, labels: MonarchDashboardGridLabels) -> String {
        labels.moveEarlier?(widget) ?? MonarchDashboardGridStrings.moveEarlier(widget)
    }

    /// The label of the later button.
    static func moveLaterLabel(_ widget: String, labels: MonarchDashboardGridLabels) -> String {
        labels.moveLater?(widget) ?? MonarchDashboardGridStrings.moveLater(widget)
    }

    /// The announcement after a button move, the position from 1.
    static func movedAnnouncement(_ widget: String, to index: Int, count: Int, labels: MonarchDashboardGridLabels)
        -> String
    {
        labels.moved?(widget, index + 1, count) ?? MonarchDashboardGridStrings.moved(widget, index + 1, count)
    }

    /// The announcement after a drop, the position from 1.
    static func droppedAnnouncement(_ widget: String, to index: Int, count: Int, labels: MonarchDashboardGridLabels)
        -> String
    {
        labels.dropped?(widget, index + 1, count) ?? MonarchDashboardGridStrings.dropped(widget, index + 1, count)
    }
}
