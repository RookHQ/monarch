import MonarchUI
import SwiftUI

struct MonarchAreaChartSection: CataloguePage {
    static let title = "AreaChart"

    @State private var stacking = MonarchAreaChartStacking.stacked
    @State private var curve = MonarchLineChartCurve.linear
    @State private var status = MonarchChartFrameStatus.ready
    @State private var hidden: Set<String> = []

    private let days: [Date] = (0..<14).map { Date(timeIntervalSince1970: 1_772_366_400 + Double($0) * 86_400) }
    private let web: [Double?] = [420, 460, 445, 480, 610, 640, 600, 520, 560, 610, 600, 640, 690, 670]
    private let ios: [Double?] = [210, 230, 220, 240, 300, 320, 310, 260, 280, 300, 310, 330, 350, 340]
    private let android: [Double?] = [150, 160, 170, 160, 210, 220, 230, 190, 200, 220, 230, 240, 250, 260]
    private let users: [Double?] = (0..<14).map { Double(1_000 + $0 * 85) }

    var body: some View {
        CatalogueCaption("Traffic by platform. Touch the plot.")
        Picker("Stacking", selection: $stacking) {
            Text("Overlap").tag(MonarchAreaChartStacking.overlap)
            Text("Stacked").tag(MonarchAreaChartStacking.stacked)
            Text("100%").tag(MonarchAreaChartStacking.expand)
        }
        .pickerStyle(.segmented)
        Picker("Curve", selection: $curve) {
            Text("Linear").tag(MonarchLineChartCurve.linear)
            Text("Monotone").tag(MonarchLineChartCurve.monotone)
        }
        .pickerStyle(.segmented)
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchChartFrameStatus.ready)
            Text("Loading").tag(MonarchChartFrameStatus.loading)
            Text("Empty").tag(MonarchChartFrameStatus.empty)
            Text("Error").tag(MonarchChartFrameStatus.error)
        }
        .pickerStyle(.segmented)
        MonarchAreaChart(
            "Traffic",
            dates: days,
            series: [
                MonarchLineChartSeries("web", label: "Web", values: web),
                MonarchLineChartSeries("ios", label: "iOS", values: ios),
                MonarchLineChartSeries("android", label: "Android", values: android),
            ],
            description: "Daily sessions by platform",
            xTitle: "Day",
            stacking: stacking,
            curve: curve,
            status: status,
            hiddenSeries: $hidden,
            emptyMessage: "Sessions appear after the first visit.",
            onRetry: { status = .ready }
        )

        CatalogueCaption("One series with a goal")
        MonarchAreaChart(
            "Users",
            dates: days,
            series: [MonarchLineChartSeries("users", label: "Users", values: users, color: .data3)],
            referenceLines: [MonarchLineChartReferenceLine(1_800, label: "Goal")],
            curve: .monotone,
            height: 180
        )
    }
}
