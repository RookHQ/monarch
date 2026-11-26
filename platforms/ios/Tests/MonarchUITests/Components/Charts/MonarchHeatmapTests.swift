import Accessibility
import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let days = ["Mon", "Tue", "Wed"].map { MonarchHeatmapAxisItem($0) }
private let hours = [
    MonarchHeatmapAxisItem("9:00", key: "9"),
    MonarchHeatmapAxisItem("10:00", key: "10"),
    MonarchHeatmapAxisItem("11:00", key: "11"),
]
private let cells = [
    MonarchHeatmapCell(row: "Mon", column: "9", value: 0),
    MonarchHeatmapCell(row: "Mon", column: "10", value: 4),
    MonarchHeatmapCell(row: "Mon", column: "11", value: 8, note: "Launch"),
    MonarchHeatmapCell(row: "Tue", column: "9", value: 12),
    MonarchHeatmapCell(row: "Tue", column: "10", value: 16, valueLabel: "16 tickets"),
    MonarchHeatmapCell(row: "Wed", column: "9", value: nil),
]

private func week() -> (rows: [MonarchHeatmapAxisItem], columns: [MonarchHeatmapAxisItem], cells: [MonarchHeatmapCell])
{
    let rows = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
        .map { MonarchHeatmapAxisItem($0) }
    let columns = (0..<24).map { MonarchHeatmapAxisItem("\($0)") }
    let cells = rows.enumerated().flatMap { r, row in
        columns.enumerated().map { c, column in
            MonarchHeatmapCell(row: row.key, column: column.key, value: Double((r * 7 + c * 3) % 20))
        }
    }
    return (rows, columns, cells)
}

enum MonarchHeatmapFixtures: ComponentFixtures {
    static let id = "heatmap"
    static let fixtures = [
        Fixture("small matrix with values, notes and a missing cell") {
            MonarchHeatmap(
                "Support tickets",
                rows: days,
                columns: hours,
                cells: cells,
                description: "This week",
                showsValues: true
            )
        },
        Fixture("week by hour, every colour slot") {
            let data = week()
            VStack {
                ForEach(MonarchChartColor.allCases, id: \.self) { color in
                    MonarchHeatmap("Activity", rows: data.rows, columns: data.columns, cells: data.cells, color: color)
                }
            }
        },
        Fixture("fixed thresholds, no legend, selectable, long labels") {
            MonarchHeatmap(
                "Attendance by team and week across the whole organisation",
                rows: [
                    MonarchHeatmapAxisItem("Platform engineering and infrastructure"),
                    MonarchHeatmapAxisItem("Design"),
                ],
                columns: hours,
                cells: cells.map {
                    MonarchHeatmapCell(
                        row: $0.row == "Mon" ? "Platform engineering and infrastructure" : "Design",
                        column: $0.column,
                        value: $0.value
                    )
                },
                thresholds: [2, 5, 10],
                showsLegend: false,
                formatValue: { "\(Int($0)) h" },
                onSelect: { _ in }
            )
        },
        Fixture("empty, loading, error") {
            VStack {
                MonarchHeatmap("Activity", rows: days, columns: hours, cells: [], emptyMessage: "No activity yet.")
                MonarchHeatmap("Activity", rows: days, columns: hours, cells: cells, status: .loading)
                MonarchHeatmap("Activity", rows: days, columns: hours, cells: cells, status: .error, onRetry: {})
            }
        },
        Fixture("legend with no-data step") {
            MonarchHeatmapLegend(
                color: .data2,
                showsNoData: true,
                steps: ["No data", "0", "Under 4", "4 to 8", "8 to 12", "12 or more"],
                labels: MonarchHeatmapLabels()
            )
        },
    ]
}

@Suite("MonarchHeatmap")
struct MonarchHeatmapTests {
    @Test("Levels: none for missing, 0 for zero or less, 1 to 4 by quarters of the largest value")
    func levels() {
        let scale = MonarchHeatmapTokens.scale(values: [0, 4, 8, 12, 16, nil], thresholds: nil)
        #expect(scale.thresholds == [4, 8, 12])
        #expect(scale.level(nil) == nil)
        #expect(scale.level(.nan) == nil)
        #expect(scale.level(-3) == 0)
        #expect(scale.level(0) == 0)
        #expect(scale.level(1) == 1)
        #expect(scale.level(4) == 2)
        #expect(scale.level(8) == 3)
        #expect(scale.level(12) == 4)
        #expect(scale.level(100) == 4)
    }

    @Test("Given thresholds are used; with only zeros every positive value is level 1")
    func thresholds() {
        let fixed = MonarchHeatmapTokens.scale(values: [100], thresholds: [2, 5, 10, 20])
        #expect(fixed.thresholds == [2, 5, 10])
        #expect(fixed.level(6) == 3)
        let zeros = MonarchHeatmapTokens.scale(values: [0, 0], thresholds: nil)
        #expect(zeros.level(3) == 1)
    }

    @Test("Fills: muted at 0, the data colour at 4, mixed in between, none without data")
    func fills() {
        let base = MonarchRGBA(red: 1, green: 0, blue: 0)
        let muted = MonarchRGBA(red: 0, green: 0, blue: 1)
        #expect(MonarchHeatmapTokens.fill(level: nil, base: base, muted: muted) == nil)
        #expect(MonarchHeatmapTokens.fill(level: 0, base: base, muted: muted) == muted)
        #expect(MonarchHeatmapTokens.fill(level: 4, base: base, muted: muted) == base)
        #expect(
            MonarchHeatmapTokens.fill(level: 2, base: base, muted: muted) == MonarchRGBA(red: 0.5, green: 0, blue: 0.5)
        )
        let one = MonarchHeatmapTokens.fill(level: 1, base: base, muted: muted)
        #expect(one?.red == 0.25)
        #expect(one?.blue == 0.75)
    }

    @Test("Every step is distinct from the next in every theme and slot")
    func stepsAreDistinct() {
        for theme in [MonarchTokens.light, MonarchTokens.dark] {
            for color in MonarchChartColor.allCases {
                let base = theme.colors[keyPath: MonarchChartLegendTokens.role(color)]
                let fills = (0..<MonarchHeatmapTokens.levelCount).compactMap {
                    MonarchHeatmapTokens.fill(level: $0, base: base, muted: theme.colors.bgMuted)
                }
                #expect(Set(fills).count == MonarchHeatmapTokens.levelCount)
            }
        }
    }

    @Test("Values sit on a chip on the two strongest levels")
    func chips() {
        #expect(!MonarchHeatmapTokens.usesChip(level: nil))
        #expect(!MonarchHeatmapTokens.usesChip(level: 2))
        #expect(MonarchHeatmapTokens.usesChip(level: 3))
        #expect(MonarchHeatmapTokens.usesChip(level: 4))
    }

    @Test("A ready chart without any value, rows or columns is empty")
    func status() {
        #expect(MonarchHeatmapTokens.status(.ready, rows: 3, columns: 3, cells: cells) == .ready)
        #expect(MonarchHeatmapTokens.status(.ready, rows: 3, columns: 3, cells: []) == .empty)
        #expect(
            MonarchHeatmapTokens.status(
                .ready,
                rows: 1,
                columns: 1,
                cells: [MonarchHeatmapCell(row: "a", column: "b", value: nil)]
            )
                == .empty
        )
        #expect(MonarchHeatmapTokens.status(.ready, rows: 0, columns: 3, cells: cells) == .empty)
        #expect(MonarchHeatmapTokens.status(.loading, rows: 0, columns: 0, cells: []) == .loading)
    }

    @Test("Cells are found by row and column; a missing pair has no data")
    func lookup() {
        let map = MonarchHeatmapTokens.lookup(cells)
        #expect(MonarchHeatmapTokens.cell(map, row: "Tue", column: "10").value == 16)
        let missing = MonarchHeatmapTokens.cell(map, row: "Wed", column: "11")
        #expect(missing.value == nil)
        #expect(missing.row == "Wed")
    }

    @Test("Values are written with the cell's text, the format, the note, or no value")
    func valueText() {
        let format: (Double) -> String = { "\(Int($0))" }
        #expect(MonarchHeatmapTokens.valueText(cells[1], format: format, noValue: "No value") == "4")
        #expect(MonarchHeatmapTokens.valueText(cells[4], format: format, noValue: "No value") == "16 tickets")
        #expect(MonarchHeatmapTokens.valueText(cells[5], format: format, noValue: "No value") == "No value")
        #expect(MonarchHeatmapTokens.valueWithNote(cells[2], format: format, noValue: "No value") == "8, Launch")
    }

    @Test("A cell is read as its row, column and value; the summary names the size and the highest cell")
    func spoken() {
        let labels = MonarchHeatmapLabels()
        #expect(MonarchHeatmapTokens.describe(row: "Mon", column: "9:00", value: "4", labels: labels) == "Mon, 9:00: 4")
        let map = MonarchHeatmapTokens.lookup(cells)
        let best = MonarchHeatmapTokens.highest(rows: days, columns: hours, map: map)
        #expect(best?.row == 1)
        #expect(best?.column == 1)
        #expect(
            MonarchHeatmapTokens.summary(
                rows: "3 rows",
                columns: "3 columns",
                highest: "Tue, 10:00: 16 tickets",
                labels: labels
            )
                == "3 rows by 3 columns. Highest: Tue, 10:00: 16 tickets"
        )
        #expect(
            MonarchHeatmapTokens.summary(rows: "1 row", columns: "3 columns", highest: nil, labels: labels)
                == "1 row by 3 columns"
        )
        let custom = MonarchHeatmapLabels(cell: "%3$@ (%1$@ %2$@)")
        #expect(MonarchHeatmapTokens.describe(row: "Mon", column: "9", value: "4", labels: custom) == "4 (Mon 9)")
    }

    @Test("Counts take the one or many form")
    func counts() {
        #expect(MonarchHeatmapTokens.count(1, text: "1", one: "%1$@ row", many: "%1$@ rows") == "1 row")
        #expect(MonarchHeatmapTokens.count(2, text: "2", one: "%1$@ row", many: "%1$@ rows") == "2 rows")
    }

    @Test("The table has a row per row; the audio graph has the columns across and a series per row")
    func tables() {
        let map = MonarchHeatmapTokens.lookup(cells)
        let format: (Double) -> String = { "\(Int($0))" }
        let table = MonarchHeatmapTokens.table(
            rows: days,
            columns: hours,
            map: map,
            format: format,
            noValue: "No value",
            labels: MonarchHeatmapLabels()
        )
        #expect(table.categoryTitle == "Row")
        #expect(table.categories == ["Mon", "Tue", "Wed"])
        #expect(table.series.map(\.name) == ["9:00", "10:00", "11:00"])
        #expect(table.series[2].formattedValues == ["8, Launch", "", ""])
        #expect(table.series[0].values == [0, 12, nil])
        let audio = MonarchHeatmapTokens.audioGraphTable(
            rows: days,
            columns: hours,
            map: map,
            format: format,
            noValue: "No value",
            categoryTitle: nil
        )
        #expect(audio.categories == ["9:00", "10:00", "11:00"])
        #expect(audio.series.map(\.name) == ["Mon", "Tue", "Wed"])
    }

    @Test("VoiceOver's chart descriptor leaves cells without data out")
    @MainActor
    func descriptor() {
        let map = MonarchHeatmapTokens.lookup(cells)
        let audio = MonarchHeatmapTokens.audioGraphTable(
            rows: days,
            columns: hours,
            map: map,
            format: { "\(Int($0))" },
            noValue: "No value",
            categoryTitle: nil
        )
        let chart = MonarchChartFrameDescriptor(title: "Tickets", summary: "3 rows", table: audio).makeChartDescriptor()
        #expect(chart.series.count == 3)
        #expect(chart.series[0].dataPoints.count == 3)
        #expect(chart.series[2].dataPoints.isEmpty)
        #expect(chart.yAxis?.range == 0...16)
    }

    @Test("The legend reads every step, with the no-data step first when shown")
    func legend() {
        let scale = MonarchHeatmapTokens.Scale(thresholds: [4, 8, 12])
        let steps = MonarchHeatmapTokens.legendSteps(
            scale: scale,
            format: { "\(Int($0))" },
            showsNoData: true,
            labels: MonarchHeatmapLabels()
        )
        #expect(steps == ["No data", "0", "Under 4", "4 to 8", "8 to 12", "12 or more"])
    }

    @Test("VoiceOver adjustment visits the cells row by row and stops at the ends")
    func adjust() {
        let first = MonarchHeatmapTokens.adjusted(nil, forward: true, rows: 2, columns: 3)
        #expect(first?.row == 0 && first?.column == 0)
        let last = MonarchHeatmapTokens.adjusted(nil, forward: false, rows: 2, columns: 3)
        #expect(last?.row == 1 && last?.column == 2)
        let wrap = MonarchHeatmapTokens.adjusted((0, 2), forward: true, rows: 2, columns: 3)
        #expect(wrap?.row == 1 && wrap?.column == 0)
        let end = MonarchHeatmapTokens.adjusted((1, 2), forward: true, rows: 2, columns: 3)
        #expect(end?.row == 1 && end?.column == 2)
        let start = MonarchHeatmapTokens.adjusted((0, 0), forward: false, rows: 2, columns: 3)
        #expect(start?.row == 0 && start?.column == 0)
        #expect(MonarchHeatmapTokens.adjusted(nil, forward: true, rows: 0, columns: 3) == nil)
    }

    @Test("Cells are square and never larger than 36 points")
    func aspect() {
        #expect(MonarchHeatmapTokens.plotAspectRatio(rows: 7, columns: 24) == 24.0 / 7)
        #expect(MonarchHeatmapTokens.plotAspectRatio(rows: 0, columns: 0) == 1)
        #expect(MonarchHeatmapTokens.maximumPlotWidth(columns: 4) == 144)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchHeatmapStrings.rowHeader == "Row")
        #expect(MonarchHeatmapStrings.less == "Less")
        #expect(MonarchHeatmapStrings.more == "More")
        #expect(MonarchHeatmapStrings.noData == "No data")
        for format in [
            MonarchHeatmapStrings.rangeBelow, MonarchHeatmapStrings.range, MonarchHeatmapStrings.rangeAbove,
            MonarchHeatmapStrings.size, MonarchHeatmapStrings.highest, MonarchHeatmapStrings.cell,
            MonarchHeatmapStrings.rowCountOne, MonarchHeatmapStrings.rowCount,
            MonarchHeatmapStrings.columnCountOne, MonarchHeatmapStrings.columnCount,
        ] {
            #expect(!format.hasPrefix("monarch."))
            #expect(format.contains("%1$@"))
        }
    }

    @Test("A week by hour lays out with height for its rows")
    @MainActor
    func layout() {
        let data = week()
        let size = Host.layout(MonarchHeatmap("Activity", rows: data.rows, columns: data.columns, cells: data.cells))
        #expect(size.height >= MonarchHeatmapTokens.minimumHeight)
    }
}
