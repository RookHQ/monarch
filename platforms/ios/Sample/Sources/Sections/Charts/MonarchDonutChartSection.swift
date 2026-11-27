import MonarchUI
import SwiftUI

struct MonarchDonutChartSection: CataloguePage {
    static let title = "DonutChart"

    @State private var legendContent = MonarchDonutChartLegendContent.both
    @State private var status = MonarchChartFrameStatus.ready
    @State private var maxSegments = 6

    private let devices = [
        MonarchDonutChartSegment("laptop", label: "Laptops", value: 412),
        MonarchDonutChartSegment("phone", label: "Phones", value: 268),
        MonarchDonutChartSegment("monitor", label: "Monitors", value: 131),
        MonarchDonutChartSegment("tablet", label: "Tablets", value: 54),
        MonarchDonutChartSegment("other", label: "Accessories", value: 9),
    ]

    var body: some View {
        Picker("Legend", selection: $legendContent) {
            Text("Both").tag(MonarchDonutChartLegendContent.both)
            Text("Value").tag(MonarchDonutChartLegendContent.value)
            Text("Percent").tag(MonarchDonutChartLegendContent.percent)
            Text("None").tag(MonarchDonutChartLegendContent.none)
        }
        .pickerStyle(.segmented)
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchChartFrameStatus.ready)
            Text("Loading").tag(MonarchChartFrameStatus.loading)
            Text("Empty").tag(MonarchChartFrameStatus.empty)
            Text("Error").tag(MonarchChartFrameStatus.error)
        }
        .pickerStyle(.segmented)
        Stepper("Most slices: \(maxSegments)", value: $maxSegments, in: 2...6)

        MonarchDonutChart(
            "Assets by type",
            segments: devices,
            description: "All active assets",
            centerLabel: "assets",
            maxSegments: maxSegments,
            legendContent: legendContent,
            status: status,
            emptyMessage: "Assets appear after the first device sync.",
            onRetry: { status = .ready }
        )

        CatalogueCaption("A custom centre value, larger ring")
        MonarchDonutChart(
            "Storage",
            segments: [
                MonarchDonutChartSegment("files", label: "Files", value: 820),
                MonarchDonutChartSegment("backups", label: "Backups", value: 540),
                MonarchDonutChartSegment("free", label: "Free", value: 640, color: .neutral),
            ],
            centerValue: "68%",
            centerLabel: "used",
            sort: .none,
            size: 200,
            formatValue: { "\($0.formatted()) GB" }
        )
    }
}
