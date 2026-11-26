import Charts
import SwiftUI

/// A matrix of values as shaded cells, such as activity by weekday and hour
/// or attendance by team and week.
///
/// ```swift
/// MonarchHeatmap(
///     "Support tickets",
///     rows: [MonarchHeatmapAxisItem("Mon"), MonarchHeatmapAxisItem("Tue")],
///     columns: [MonarchHeatmapAxisItem("9:00"), MonarchHeatmapAxisItem("10:00")],
///     cells: [
///         MonarchHeatmapCell(row: "Mon", column: "9:00", value: 12),
///         MonarchHeatmapCell(row: "Tue", column: "10:00", value: 4),
///     ]
/// )
/// ```
///
/// Values are binned into four steps of one data colour mixed into the muted
/// background, with a "Less … More" legend; zero is the muted background and
/// a missing value is an outlined empty cell. The matrix is a Swift Charts
/// chart of `RectangleMark`s with square cells up to 36 points, inside a
/// ``MonarchChartFrame`` with loading, empty and error states and "View as
/// table". Tapping a cell outlines it and writes its row, column and value
/// under the matrix.
///
/// Accessibility: the matrix is one adjustable element named by the generated
/// summary (its size and highest cell). Swiping up and down moves through the
/// cells row by row and reads each one; with `onSelect`, a double tap selects
/// it. It carries an accessibility chart descriptor for Audio Graphs, with
/// the columns along the x axis and one series per row. The legend is read
/// as its steps.
public struct MonarchHeatmap: View {
    private let title: String
    private let rows: [MonarchHeatmapAxisItem]
    private let columns: [MonarchHeatmapAxisItem]
    private let cells: [MonarchHeatmapCell]
    private let description: String?
    private let status: MonarchChartFrameStatus
    private let color: MonarchChartColor
    private let thresholds: [Double]?
    private let showsValues: Bool
    private let showsLegend: Bool
    private let summary: String?
    private let emptyMessage: String?
    private let footer: String?
    private let labels: MonarchHeatmapLabels
    private let formatValue: ((Double) -> String)?
    private let onRetry: (@MainActor () -> Void)?
    private let onSelect: (@MainActor (MonarchHeatmapCell) -> Void)?

    @State private var active: Position?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A row and column index.
    private struct Position: Hashable {
        let row: Int
        let column: Int
    }

    /// A heatmap.
    ///
    /// - Parameters:
    ///   - title: The name of the chart, already localised.
    ///   - rows: The rows, top to bottom, such as weekdays.
    ///   - columns: The columns, leading to trailing, such as hours.
    ///   - cells: The values, in any order. A row and column pair without a cell is "no data".
    ///   - description: One line under the title, such as the period.
    ///   - status: Whether the chart, a placeholder, an empty state or an error shows.
    ///     A ready chart without any value shows the empty state.
    ///   - color: The data colour of the steps.
    ///   - thresholds: Three ascending values where levels 2, 3 and 4 start. Quarters of the largest value when `nil`.
    ///   - showsValues: Whether each value is written in its cell when the cell has room.
    ///   - showsLegend: Whether the "Less … More" legend shows under the matrix.
    ///   - summary: Replaces the generated summary that VoiceOver reads for the matrix.
    ///   - emptyMessage: The empty-state message.
    ///   - footer: Notes under the chart, such as the source.
    ///   - labels: Replacements for the built-in strings.
    ///   - formatValue: Writes a value. The default is a number in the environment's locale.
    ///   - onRetry: Shows Try again in the error status and runs when it is pressed.
    ///   - onSelect: Runs with the cell (its value `nil` when missing) when a cell is tapped or selected with VoiceOver.
    public init(
        _ title: String,
        rows: [MonarchHeatmapAxisItem],
        columns: [MonarchHeatmapAxisItem],
        cells: [MonarchHeatmapCell],
        description: String? = nil,
        status: MonarchChartFrameStatus = .ready,
        color: MonarchChartColor = .data1,
        thresholds: [Double]? = nil,
        showsValues: Bool = false,
        showsLegend: Bool = true,
        summary: String? = nil,
        emptyMessage: String? = nil,
        footer: String? = nil,
        labels: MonarchHeatmapLabels = MonarchHeatmapLabels(),
        formatValue: ((Double) -> String)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onSelect: (@MainActor (MonarchHeatmapCell) -> Void)? = nil
    ) {
        self.title = title
        self.rows = rows
        self.columns = columns
        self.cells = cells
        self.description = description
        self.status = status
        self.color = color
        self.thresholds = thresholds
        self.showsValues = showsValues
        self.showsLegend = showsLegend
        self.summary = summary
        self.emptyMessage = emptyMessage
        self.footer = footer
        self.labels = labels
        self.formatValue = formatValue
        self.onRetry = onRetry
        self.onSelect = onSelect
    }

    /// The content of the view.
    public var body: some View {
        let resolved = MonarchHeatmapTokens.status(status, rows: rows.count, columns: columns.count, cells: cells)
        let map = MonarchHeatmapTokens.lookup(cells)
        MonarchChartFrame(
            title,
            description: description,
            status: resolved,
            table: MonarchHeatmapTokens.table(
                rows: rows,
                columns: columns,
                map: map,
                format: format,
                noValue: noValue,
                labels: labels
            ),
            emptyMessage: emptyMessage,
            footer: footer,
            height: MonarchHeatmapTokens.frameHeight(rows: rows.count),
            contentAccessibility: .contain,
            labels: labels.frame,
            onRetry: onRetry
        ) {
            chart(map: map)
        }
    }

    private var noValue: String {
        labels.frame.noValue ?? MonarchChartFrameStrings.noValue
    }

    private func format(_ value: Double) -> String {
        formatValue?(value) ?? value.formatted(.number.locale(locale))
    }

    /// The current cell, if its position is still inside the matrix.
    private var current: Position? {
        guard let active, rows.indices.contains(active.row), columns.indices.contains(active.column) else {
            return nil
        }
        return active
    }

    private func describe(_ position: Position, map: [String: MonarchHeatmapCell]) -> String {
        let cell = MonarchHeatmapTokens.cell(map, row: rows[position.row].key, column: columns[position.column].key)
        return MonarchHeatmapTokens.describe(
            row: rows[position.row].label,
            column: columns[position.column].label,
            value: MonarchHeatmapTokens.valueWithNote(cell, format: format, noValue: noValue),
            labels: labels
        )
    }

    private func chart(map: [String: MonarchHeatmapCell]) -> some View {
        let scale = MonarchHeatmapTokens.scale(values: cells.map(\.value), thresholds: thresholds)
        let current = current
        let spoken = summary ?? generatedSummary(map: map)
        return VStack(alignment: .leading, spacing: MonarchHeatmapTokens.sectionSpacing) {
            MonarchHeatmapPlot(
                rows: rows,
                columns: columns,
                map: map,
                scale: scale,
                color: color,
                showsValues: showsValues,
                active: current.map { ($0.row, $0.column) },
                valueText: { MonarchHeatmapTokens.valueText($0, format: format, noValue: noValue) },
                onTap: { row, column in select(Position(row: row, column: column), map: map) }
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(spoken)
            .accessibilityValue(current.map { describe($0, map: map) } ?? "")
            .accessibilityAddTraits(onSelect == nil ? [] : .isButton)
            .accessibilityAdjustableAction { direction in
                let next = MonarchHeatmapTokens.adjusted(
                    current.map { ($0.row, $0.column) },
                    forward: direction == .increment,
                    rows: rows.count,
                    columns: columns.count
                )
                active = next.map { Position(row: $0.row, column: $0.column) }
            }
            .accessibilityAction {
                if let current { select(current, map: map) }
            }
            .accessibilityChartDescriptor(
                MonarchChartFrameDescriptor(
                    title: title,
                    summary: spoken,
                    table: MonarchHeatmapTokens.audioGraphTable(
                        rows: rows,
                        columns: columns,
                        map: map,
                        format: format,
                        noValue: noValue,
                        categoryTitle: nil
                    )
                )
            )
            if let current {
                Text(describe(current, map: map))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityHidden(true)
            }
            if showsLegend {
                MonarchHeatmapLegend(
                    color: color,
                    showsNoData: cells.count < rows.count * columns.count,
                    steps: MonarchHeatmapTokens.legendSteps(
                        scale: scale,
                        format: format,
                        showsNoData: cells.count < rows.count * columns.count,
                        labels: labels
                    ),
                    labels: labels
                )
            }
        }
    }

    private func select(_ position: Position, map: [String: MonarchHeatmapCell]) {
        active = position
        guard let onSelect, rows.indices.contains(position.row), columns.indices.contains(position.column) else {
            return
        }
        onSelect(MonarchHeatmapTokens.cell(map, row: rows[position.row].key, column: columns[position.column].key))
    }

    private func generatedSummary(map: [String: MonarchHeatmapCell]) -> String {
        let highest = MonarchHeatmapTokens.highest(rows: rows, columns: columns, map: map)
        return MonarchHeatmapTokens.summary(
            rows: MonarchHeatmapTokens.count(
                rows.count,
                text: rows.count.formatted(.number.locale(locale)),
                one: labels.rowCountOne ?? MonarchHeatmapStrings.rowCountOne,
                many: labels.rowCount ?? MonarchHeatmapStrings.rowCount
            ),
            columns: MonarchHeatmapTokens.count(
                columns.count,
                text: columns.count.formatted(.number.locale(locale)),
                one: labels.columnCountOne ?? MonarchHeatmapStrings.columnCountOne,
                many: labels.columnCount ?? MonarchHeatmapStrings.columnCount
            ),
            highest: highest.map { describe(Position(row: $0.row, column: $0.column), map: map) },
            labels: labels
        )
    }
}

#if DEBUG
    #Preview("MonarchHeatmap") {
        let days = ["Mon", "Tue", "Wed"].map { MonarchHeatmapAxisItem($0) }
        let hours = ["9", "10", "11", "12"].map { MonarchHeatmapAxisItem($0) }
        let cells = days.enumerated().flatMap { r, day in
            hours.enumerated().map { c, hour in
                MonarchHeatmapCell(row: day.key, column: hour.key, value: Double((r + 1) * (c + 1)))
            }
        }
        MonarchPreviewMatrix {
            VStack {
                MonarchHeatmap("Tickets", rows: days, columns: hours, cells: cells, showsValues: true)
                MonarchHeatmap("Tickets", rows: days, columns: hours, cells: Array(cells.prefix(5)), color: .data3)
            }
        }
    }
#endif
