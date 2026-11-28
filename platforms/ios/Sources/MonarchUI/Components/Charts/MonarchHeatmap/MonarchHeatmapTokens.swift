import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchHeatmap``.
enum MonarchHeatmapTokens {
    /// The largest side of a cell: 36 points.
    static let maximumCellSize: CGFloat = MonarchTokens.Space.space8 + MonarchTokens.Space.space1
    /// The share of its slot that a cell fills; the rest is the gap between cells.
    static let cellRatio: CGFloat = 0.92
    /// The share of its slot inside the outline of an empty or selected cell.
    static let innerRatio: CGFloat = 0.78
    /// The corner radius of a cell.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.xs
    /// The least cell side, before Dynamic Type, that has room for a value.
    static let minimumValueCell: CGFloat = MonarchTokens.Space.space6
    /// How far a value's text may shrink to fit its cell.
    static let valueMinimumScale: CGFloat = 0.8
    /// Padding before and after a value on its chip.
    static let chipPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The side of a legend swatch before Dynamic Type.
    static let swatchSize: CGFloat = MonarchTokens.Space.space3
    /// The gap between legend swatches.
    static let swatchSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the legend's words and its swatches.
    static let legendSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the plot, the cell details and the legend.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The least height kept for the chart: 120 points.
    static let minimumHeight: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space6
    /// The number of colour levels, 0 to 4.
    static let levelCount = 5
    /// How much of the base colour each step mixes into the muted background, levels 1 to 3.
    static let levelMix: [Double] = [0.25, 0.5, 0.75]
    /// The share of the largest value where levels 2, 3 and 4 start, without thresholds.
    static let defaultCuts: [Double] = [0.25, 0.5, 0.75]

    /// The colour level of a value: `nil` for no data, 0 for zero or less, 1 to 4 for positive values.
    struct Scale: Equatable {
        /// The lower bounds of levels 2, 3 and 4.
        let thresholds: [Double]

        /// The level of a value.
        func level(_ value: Double?) -> Int? {
            guard let value, value.isFinite else { return nil }
            if value <= 0 { return 0 }
            let low = thresholds[0]
            let middle = thresholds[1]
            let high = thresholds[2]
            if high > 0, value >= high { return 4 }
            if middle > 0, value >= middle { return 3 }
            if low > 0, value >= low { return 2 }
            return 1
        }
    }

    /// The scale: three given ascending cut points, or quarters of the largest value.
    static func scale(values: [Double?], thresholds: [Double]?) -> Scale {
        if let thresholds, thresholds.count >= 3 {
            return Scale(thresholds: Array(thresholds.prefix(3)))
        }
        let largest = values.compactMap { $0 }.filter(\.isFinite).max() ?? 0
        return Scale(thresholds: defaultCuts.map { max(0, largest) * $0 })
    }

    /// Mixes `amount` of one colour into another, channel by channel.
    static func mix(_ color: MonarchRGBA, into background: MonarchRGBA, amount: Double) -> MonarchRGBA {
        let share = min(1, max(0, amount))
        func channel(_ a: Double, _ b: Double) -> Double { b + (a - b) * share }
        return MonarchRGBA(
            red: channel(color.red, background.red),
            green: channel(color.green, background.green),
            blue: channel(color.blue, background.blue),
            alpha: channel(color.alpha, background.alpha)
        )
    }

    /// The fill of a level: the muted background at 0, the base colour at 4,
    /// and the base mixed into the background in between. `nil` for no data.
    static func fill(level: Int?, base: MonarchRGBA, muted: MonarchRGBA) -> MonarchRGBA? {
        guard let level else { return nil }
        switch level {
        case ...0: return muted
        case 4...: return base
        default: return mix(base, into: muted, amount: levelMix[level - 1])
        }
    }

    /// Whether a value is drawn on a surface chip: the two strongest levels,
    /// where no single text colour reads on every palette slot.
    static func usesChip(level: Int?) -> Bool {
        (level ?? 0) >= 3
    }

    /// The status shown: empty when the chart is ready but has no value to show.
    static func status(
        _ status: MonarchChartFrameStatus,
        rows: Int,
        columns: Int,
        cells: [MonarchHeatmapCell]
    ) -> MonarchChartFrameStatus {
        let hasData = cells.contains { $0.value?.isFinite == true }
        return status == .ready && (!hasData || rows == 0 || columns == 0) ? .empty : status
    }

    /// The height of the chart area: a row of about the cell size per row plus the column labels, so cells are not
    /// squashed flat when there are few rows.
    static func frameHeight(rows: Int) -> CGFloat {
        max(
            minimumHeight,
            CGFloat(rows) * (MonarchTokens.Space.space8 - MonarchTokens.Space.space0_5) + MonarchTokens.Space.space10
        )
    }

    /// The aspect ratio of the plot, so cells are square.
    static func plotAspectRatio(rows: Int, columns: Int) -> CGFloat {
        guard rows > 0, columns > 0 else { return 1 }
        return CGFloat(columns) / CGFloat(rows)
    }

    /// The widest the plot gets, so cells are never larger than the maximum.
    static func maximumPlotWidth(columns: Int) -> CGFloat {
        CGFloat(max(1, columns)) * maximumCellSize
    }

    /// Looks up cells by row and column key. A later cell for the same pair wins.
    static func lookup(_ cells: [MonarchHeatmapCell]) -> [String: MonarchHeatmapCell] {
        var map: [String: MonarchHeatmapCell] = [:]
        for cell in cells {
            map[key(row: cell.row, column: cell.column)] = cell
        }
        return map
    }

    /// The lookup key of a position.
    static func key(row: String, column: String) -> String {
        "\(row)\u{0}\(column)"
    }

    /// The cell at a position, or a cell without data.
    static func cell(_ map: [String: MonarchHeatmapCell], row: String, column: String) -> MonarchHeatmapCell {
        map[key(row: row, column: column)] ?? MonarchHeatmapCell(row: row, column: column, value: nil)
    }

    /// The value as written: the cell's own text, the formatted value, or the no-value text.
    static func valueText(_ cell: MonarchHeatmapCell, format: (Double) -> String, noValue: String) -> String {
        if let label = cell.valueLabel { return label }
        guard let value = cell.value, value.isFinite else { return noValue }
        return format(value)
    }

    /// The value with the cell's note, as the table and VoiceOver read it.
    static func valueWithNote(_ cell: MonarchHeatmapCell, format: (Double) -> String, noValue: String) -> String {
        let text = valueText(cell, format: format, noValue: noValue)
        guard let note = cell.note, !note.isEmpty else { return text }
        return "\(text), \(note)"
    }

    /// What VoiceOver reads for a cell: its row, its column and its value.
    static func describe(row: String, column: String, value: String, labels: MonarchHeatmapLabels) -> String {
        MonarchHeatmapStrings.fill(labels.cell ?? MonarchHeatmapStrings.cell, row, column, value)
    }

    /// The position of the highest value, the first one found row by row on a tie.
    static func highest(
        rows: [MonarchHeatmapAxisItem],
        columns: [MonarchHeatmapAxisItem],
        map: [String: MonarchHeatmapCell]
    ) -> (row: Int, column: Int)? {
        var best: (row: Int, column: Int, value: Double)?
        for (r, row) in rows.enumerated() {
            for (c, column) in columns.enumerated() {
                guard let value = map[key(row: row.key, column: column.key)]?.value, value.isFinite else { continue }
                if value > (best?.value ?? -.infinity) {
                    best = (r, c, value)
                }
            }
        }
        return best.map { ($0.row, $0.column) }
    }

    /// A count with its noun: the "one" format for 1, else the "many" format.
    static func count(_ value: Int, text: String, one: String, many: String) -> String {
        MonarchHeatmapStrings.fill(value == 1 ? one : many, text)
    }

    /// The generated summary: the size of the matrix and its highest cell.
    static func summary(rows: String, columns: String, highest: String?, labels: MonarchHeatmapLabels) -> String {
        let size = MonarchHeatmapStrings.fill(labels.size ?? MonarchHeatmapStrings.size, rows, columns)
        guard let highest else { return size }
        return size + ". " + MonarchHeatmapStrings.fill(labels.highest ?? MonarchHeatmapStrings.highest, highest)
    }

    /// The data table: one row per heatmap row and one column per heatmap column.
    static func table(
        rows: [MonarchHeatmapAxisItem],
        columns: [MonarchHeatmapAxisItem],
        map: [String: MonarchHeatmapCell],
        format: (Double) -> String,
        noValue: String,
        labels: MonarchHeatmapLabels
    ) -> MonarchChartTable {
        MonarchChartTable(
            categoryTitle: labels.rowHeader ?? MonarchHeatmapStrings.rowHeader,
            categories: rows.map(\.label),
            series: columns.map { column in
                let cells = rows.map { cell(map, row: $0.key, column: column.key) }
                return MonarchChartTableSeries(
                    column.label,
                    values: cells.map { $0.value.flatMap { $0.isFinite ? $0 : nil } },
                    formattedValues: cells.map {
                        $0.value == nil ? "" : valueWithNote($0, format: format, noValue: noValue)
                    }
                )
            }
        )
    }

    /// The table given to VoiceOver as an audio graph: the columns along the
    /// x axis and one series per row, such as hours across and one series per weekday.
    static func audioGraphTable(
        rows: [MonarchHeatmapAxisItem],
        columns: [MonarchHeatmapAxisItem],
        map: [String: MonarchHeatmapCell],
        format: (Double) -> String,
        noValue: String,
        categoryTitle: String?
    ) -> MonarchChartTable {
        MonarchChartTable(
            categoryTitle: categoryTitle ?? "",
            categories: columns.map(\.label),
            series: rows.map { row in
                let cells = columns.map { cell(map, row: row.key, column: $0.key) }
                return MonarchChartTableSeries(
                    row.label,
                    values: cells.map { $0.value.flatMap { $0.isFinite ? $0 : nil } },
                    formattedValues: cells.map { valueText($0, format: format, noValue: noValue) }
                )
            }
        )
    }

    /// The legend's steps, lightest first, as VoiceOver reads them.
    static func legendSteps(
        scale: Scale,
        format: (Double) -> String,
        showsNoData: Bool,
        labels: MonarchHeatmapLabels
    ) -> [String] {
        let low = scale.thresholds[0]
        let middle = scale.thresholds[1]
        let high = scale.thresholds[2]
        let range = labels.range ?? MonarchHeatmapStrings.range
        var steps: [String] = []
        if showsNoData { steps.append(labels.noData ?? MonarchHeatmapStrings.noData) }
        steps.append(format(0))
        steps.append(
            low > 0
                ? MonarchHeatmapStrings.fill(labels.rangeBelow ?? MonarchHeatmapStrings.rangeBelow, format(low))
                : MonarchHeatmapStrings.fill(range, format(0), format(low))
        )
        steps.append(MonarchHeatmapStrings.fill(range, format(low), format(middle)))
        steps.append(MonarchHeatmapStrings.fill(range, format(middle), format(high)))
        steps.append(MonarchHeatmapStrings.fill(labels.rangeAbove ?? MonarchHeatmapStrings.rangeAbove, format(high)))
        return steps
    }

    /// The position after an adjustment by VoiceOver: cells are visited row
    /// by row; the first swipe goes to the first cell (or the last, backwards).
    static func adjusted(
        _ position: (row: Int, column: Int)?,
        forward: Bool,
        rows: Int,
        columns: Int
    ) -> (row: Int, column: Int)? {
        guard rows > 0, columns > 0 else { return nil }
        let count = rows * columns
        guard let position else { return forward ? (0, 0) : (rows - 1, columns - 1) }
        let index = position.row * columns + position.column
        let next = min(count - 1, max(0, index + (forward ? 1 : -1)))
        return (next / columns, next % columns)
    }
}
