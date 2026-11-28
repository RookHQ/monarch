import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchChartFrame``.
enum MonarchChartFrameTokens {
    /// The gap between the header, the legend, the body and the footer.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the description.
    static let headingSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the heading and the actions, and between actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The height kept for the chart body in every status, so nothing jumps: 240 points.
    static let defaultHeight: CGFloat = MonarchTokens.Space.space20 * 3
    /// The gap between table columns.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between table rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the error icon and its text.
    static let errorIconSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The SF Symbol of the error message without a retry.
    static let errorSymbol = "exclamationmark.triangle.fill"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "chart.bar"
    /// The text drawn for a gap in the table.
    static let gapText = "–"

    /// Whether the "View as table" toggle is shown: only with a table, when
    /// it is allowed, and while the frame is ready.
    static func showsTableToggle(hasTable: Bool, isAllowed: Bool, status: MonarchChartFrameStatus) -> Bool {
        hasTable && isAllowed && status == .ready
    }

    /// Whether the table replaces the chart.
    static func showsTable(isRequested: Bool, canToggle: Bool) -> Bool {
        isRequested && canToggle
    }

    /// Whether the legend is shown: with entries, while ready and the chart is visible.
    static func showsLegend(entries: Int, status: MonarchChartFrameStatus, showsTable: Bool) -> Bool {
        entries > 0 && status == .ready && !showsTable
    }

    /// The title of the toggle for the view now showing.
    static func toggleTitle(showsTable: Bool, labels: MonarchChartFrameLabels) -> String {
        showsTable
            ? labels.viewChart ?? MonarchChartFrameStrings.viewChart
            : labels.viewTable ?? MonarchChartFrameStrings.viewTable
    }

    /// The text of one cell: the app's formatted value (or text cell), the
    /// locale's formatting of the number, or `nil` for a gap.
    static func cellText(_ series: MonarchChartTableSeries, at index: Int) -> String? {
        if series.isText {
            guard let formatted = series.formattedValues, formatted.indices.contains(index),
                !formatted[index].isEmpty
            else { return nil }
            return formatted[index]
        }
        guard series.values.indices.contains(index), let value = series.values[index] else { return nil }
        if let formatted = series.formattedValues, formatted.indices.contains(index) {
            return formatted[index]
        }
        return value.formatted()
    }

    /// Whether a series holds only text cells (a status or band per
    /// category). Such a series has nothing to plot, so the audio graph
    /// leaves it out. A numeric series with only gaps stays in, with no points.
    static func isTextOnly(_ series: MonarchChartTableSeries) -> Bool {
        series.isText
    }

    /// What VoiceOver reads for one row: the category, then each series
    /// with its value.
    static func rowAccessibilityLabel(_ table: MonarchChartTable, row: Int, noValue: String) -> String {
        let category = table.categories.indices.contains(row) ? table.categories[row] : ""
        let cells = table.series.map { "\($0.name) \(cellText($0, at: row) ?? noValue)" }
        return ([category] + cells).joined(separator: ", ")
    }

    /// The range of the value axis: every value, with zero included. A
    /// range with no height is widened by one.
    static func valueRange(_ table: MonarchChartTable) -> ClosedRange<Double> {
        let values = table.series.flatMap { $0.values.compactMap { $0 } }.filter(\.isFinite)
        let low = min(0, values.min() ?? 0)
        let high = max(0, values.max() ?? 0)
        return low < high ? low...high : low...(low + 1)
    }
}
