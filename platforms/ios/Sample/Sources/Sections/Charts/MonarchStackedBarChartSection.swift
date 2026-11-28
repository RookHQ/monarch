import MonarchUI
import SwiftUI

struct MonarchStackedBarChartSection: CataloguePage {
    static let title = "StackedBarChart"

    @State private var orientation = MonarchBarChartOrientation.vertical
    @State private var layout = MonarchBarChartLayout.stacked
    @State private var offset = MonarchStackedBarChartOffset.normal
    @State private var status = MonarchChartFrameStatus.ready
    @State private var showsValueLabels = true
    @State private var opened = "none"

    private let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun"]
    private let tickets = [
        MonarchBarChartSeries("resolved", label: "Resolved", values: [120, 132, 101, 134, 190, 230]),
        MonarchBarChartSeries("pending", label: "Pending", values: [22, 18, 30, 25, 19, 21]),
        MonarchBarChartSeries("escalated", label: "Escalated", values: [8, 6, 12, 9, 7, 5]),
    ]
    private let teams = ["Platform", "Design systems", "Growth", "Support tooling"]
    private let work = [
        MonarchBarChartSeries("done", label: "Done", values: [34, 21, 12, 27]),
        MonarchBarChartSeries("review", label: "In review", values: [6, 9, 3, 2]),
        MonarchBarChartSeries("todo", label: "To do", values: [10, 4, 18, 6], color: .neutral),
    ]

    var body: some View {
        Picker("Orientation", selection: $orientation) {
            Text("Vertical").tag(MonarchBarChartOrientation.vertical)
            Text("Horizontal").tag(MonarchBarChartOrientation.horizontal)
        }
        .pickerStyle(.segmented)
        Picker("Layout", selection: $layout) {
            Text("Stacked").tag(MonarchBarChartLayout.stacked)
            Text("Grouped").tag(MonarchBarChartLayout.grouped)
        }
        .pickerStyle(.segmented)
        Picker("Offset", selection: $offset) {
            Text("Values").tag(MonarchStackedBarChartOffset.normal)
            Text("100%").tag(MonarchStackedBarChartOffset.expand)
        }
        .pickerStyle(.segmented)
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchChartFrameStatus.ready)
            Text("Loading").tag(MonarchChartFrameStatus.loading)
            Text("Empty").tag(MonarchChartFrameStatus.empty)
            Text("Error").tag(MonarchChartFrameStatus.error)
        }
        .pickerStyle(.segmented)
        Toggle("Value labels", isOn: $showsValueLabels)

        CatalogueCaption("Touch the plot to inspect a month. Opened: \(opened)")
        MonarchStackedBarChart(
            "Tickets by status",
            categories: months,
            series: tickets,
            description: "First half of the year",
            categoryTitle: "Month",
            valueTitle: "Tickets",
            orientation: orientation,
            layout: layout,
            offset: offset,
            status: status,
            referenceLines: [MonarchBarChartReferenceLine(200, label: "Capacity")],
            showsValueLabels: showsValueLabels,
            emptyMessage: "Tickets appear after the first request.",
            footer: "Source: help desk.",
            onRetry: { status = .ready },
            onSelect: { opened = months[$0] }
        )

        CatalogueCaption("Work by status, 100%, horizontal")
        MonarchStackedBarChart(
            "Work by status",
            categories: teams,
            series: work,
            categoryTitle: "Team",
            orientation: .horizontal,
            offset: .expand
        )
    }
}
