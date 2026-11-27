import MonarchUI
import SwiftUI

struct MonarchBarChartSection: CataloguePage {
    static let title = "BarChart"

    @State private var orientation = MonarchBarChartOrientation.vertical
    @State private var layout = MonarchBarChartLayout.grouped
    @State private var status = MonarchChartFrameStatus.ready
    @State private var showsValueLabels = false
    @State private var opened = "none"

    private let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun"]
    private let series = [
        MonarchBarChartSeries("web", label: "Web", values: [1200, 1450, 1390, 1600, 1720, 1810]),
        MonarchBarChartSeries("ios", label: "iOS", values: [800, 920, 1010, nil, 1180, 1240]),
        MonarchBarChartSeries("android", label: "Android", values: [400, 380, 520, 610, 590, 700]),
    ]

    var body: some View {
        Picker("Orientation", selection: $orientation) {
            Text("Vertical").tag(MonarchBarChartOrientation.vertical)
            Text("Horizontal").tag(MonarchBarChartOrientation.horizontal)
        }
        .pickerStyle(.segmented)
        Picker("Layout", selection: $layout) {
            Text("Grouped").tag(MonarchBarChartLayout.grouped)
            Text("Stacked").tag(MonarchBarChartLayout.stacked)
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
        MonarchBarChart(
            "Revenue by platform",
            categories: months,
            series: series,
            description: "Monthly, in dollars",
            categoryTitle: "Month",
            valueTitle: "Dollars",
            orientation: orientation,
            layout: layout,
            status: status,
            referenceLines: layout == .grouped ? [MonarchBarChartReferenceLine(1500, label: "Target")] : [],
            showsValueLabels: showsValueLabels,
            emptyMessage: "Revenue appears after the first paid invoice.",
            footer: "Source: billing.",
            formatValue: { $0.formatted(.currency(code: "USD").precision(.fractionLength(0))) },
            onRetry: { status = .ready },
            onSelect: { opened = months[$0] }
        )

        CatalogueCaption("One series, current month highlighted")
        MonarchBarChart(
            "Signups",
            categories: months,
            series: [MonarchBarChartSeries("signups", label: "Signups", values: [32, 41, 38, 52, 47, 60])],
            highlightedIndex: 5,
            height: 160
        )
    }
}
