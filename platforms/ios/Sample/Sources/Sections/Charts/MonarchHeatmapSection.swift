import MonarchUI
import SwiftUI

struct MonarchHeatmapSection: CataloguePage {
    static let title = "Heatmap"

    @State private var color = MonarchChartColor.data1
    @State private var showsValues = false
    @State private var status = MonarchChartFrameStatus.ready
    @State private var selected = ""

    private let days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map { MonarchHeatmapAxisItem($0) }
    private let hours = stride(from: 8, through: 18, by: 2).map { MonarchHeatmapAxisItem("\($0):00", key: "\($0)") }

    private var cells: [MonarchHeatmapCell] {
        days.enumerated().flatMap { row, day in
            hours.enumerated().compactMap { column, hour in
                // Weekends have no data at the first hour, so the no-data cell shows.
                if row >= 5 && column == 0 { return nil }
                let value = Double((row * 5 + column * 7) % 23)
                return MonarchHeatmapCell(
                    row: day.key,
                    column: hour.key,
                    value: value,
                    note: value == 0 ? "Closed" : nil
                )
            }
        }
    }

    var body: some View {
        Picker("Colour", selection: $color) {
            Text("Data 1").tag(MonarchChartColor.data1)
            Text("Data 2").tag(MonarchChartColor.data2)
            Text("Data 3").tag(MonarchChartColor.data3)
            Text("Data 4").tag(MonarchChartColor.data4)
            Text("Data 5").tag(MonarchChartColor.data5)
        }
        .pickerStyle(.menu)
        Toggle("Values in cells", isOn: $showsValues)
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchChartFrameStatus.ready)
            Text("Loading").tag(MonarchChartFrameStatus.loading)
            Text("Empty").tag(MonarchChartFrameStatus.empty)
            Text("Error").tag(MonarchChartFrameStatus.error)
        }
        .pickerStyle(.segmented)
        MonarchHeatmap(
            "Support tickets",
            rows: days,
            columns: hours,
            cells: cells,
            description: "By weekday and hour, this week",
            status: status,
            color: color,
            showsValues: showsValues,
            emptyMessage: "Tickets appear after the first request.",
            footer: "Source: help desk.",
            onRetry: { status = .ready },
            onSelect: { cell in selected = "\(cell.row) \(cell.column)" }
        )
        if !selected.isEmpty {
            CatalogueCaption("Selected: \(selected)")
        }
    }
}
