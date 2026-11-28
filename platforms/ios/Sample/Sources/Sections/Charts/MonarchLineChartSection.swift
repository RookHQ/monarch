import MonarchUI
import SwiftUI

struct MonarchLineChartSection: CataloguePage {
    static let title = "LineChart"

    @State private var variant = MonarchLineChartVariant.line
    @State private var curve = MonarchLineChartCurve.linear
    @State private var status = MonarchChartFrameStatus.ready
    @State private var hidden: Set<String> = []
    @State private var shared: Int?

    private let days: [Date] = (0..<14).map { Date(timeIntervalSince1970: 1_772_366_400 + Double($0) * 86_400) }
    private let sessions: [Double?] = [820, 932, 901, 934, 1290, 1330, 1320, 1180, 1250, 1400, 1380, 1460, 1510, 1490]
    private let signups: [Double?] = [42, 51, 47, 60, 88, 91, 85, 70, 76, 95, 90, 99, 104, 101]
    private let latency: [Double?] = [210, 220, nil, nil, 260, 240, 410, 380, 250, 230, 225, nil, 240, 235]

    private var previous: [Double?] { sessions.map { $0.map { ($0 * 0.82).rounded() } } }

    var body: some View {
        CatalogueCaption("Two series with a previous period and a target. Touch the plot.")
        Picker("Variant", selection: $variant) {
            Text("Line").tag(MonarchLineChartVariant.line)
            Text("Area").tag(MonarchLineChartVariant.area)
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
        MonarchLineChart(
            "Sessions",
            dates: days,
            series: [
                MonarchLineChartSeries("current", label: "This period", values: sessions),
                MonarchLineChartSeries(
                    "previous",
                    label: "Previous period",
                    values: previous,
                    color: .neutral,
                    isDashed: true
                ),
            ],
            description: "Last 14 days against the previous 14",
            xTitle: "Day",
            referenceLines: [MonarchLineChartReferenceLine(1_200, label: "Target")],
            variant: variant,
            curve: curve,
            status: status,
            hiddenSeries: $hidden,
            emptyMessage: "Sessions appear after the first visit.",
            onRetry: { status = .ready }
        )

        CatalogueCaption("Area with gaps: days without traffic break the line")
        MonarchLineChart(
            "p95 latency",
            dates: days,
            series: [MonarchLineChartSeries("p95", label: "p95", values: latency, color: .data3)],
            description: "Days with no traffic are gaps",
            variant: .area,
            curve: .monotone,
            includesZero: false,
            height: 180,
            formatValue: { "\(Int($0)) ms" }
        )

        CatalogueCaption("Two charts sharing one crosshair")
        MonarchLineChart(
            "Sessions",
            dates: days,
            series: [MonarchLineChartSeries("sessions", label: "Sessions", values: sessions)],
            activeIndex: $shared,
            height: 160
        )
        MonarchLineChart(
            "Sign-ups",
            dates: days,
            series: [MonarchLineChartSeries("signups", label: "Sign-ups", values: signups, color: .data2)],
            activeIndex: $shared,
            height: 160
        )
    }
}
