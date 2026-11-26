import MonarchUI
import SwiftUI

struct MonarchBurndownChartSection: CataloguePage {
    static let title = "BurndownChart"

    @State private var showsScope = true
    @State private var status = MonarchChartFrameStatus.ready
    @State private var hidden: Set<MonarchBurndownChartLine> = []

    private let days: [Date] = (0..<10).map { index in
        Date(timeIntervalSince1970: 1_772_452_800 + Double(index + (index >= 5 ? 2 : 0)) * 86_400)
    }
    private let remaining: [Double?] = [40, 38, 35, 37, 31, 26, 24, nil, nil, nil]
    private let scope: [Double?] = [40, 40, 40, 45, 45, 43, 43, nil, nil, nil]

    var body: some View {
        CatalogueCaption("A sprint in progress, weekend left out. Touch the plot.")
        Toggle("Scope line", isOn: $showsScope)
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchChartFrameStatus.ready)
            Text("Loading").tag(MonarchChartFrameStatus.loading)
            Text("Empty").tag(MonarchChartFrameStatus.empty)
            Text("Error").tag(MonarchChartFrameStatus.error)
        }
        .pickerStyle(.segmented)
        MonarchBurndownChart(
            "Sprint 14",
            points: days.indices.map {
                MonarchBurndownChartPoint(date: days[$0], remaining: remaining[$0], scope: scope[$0])
            },
            showsScope: showsScope,
            status: status,
            hiddenLines: $hidden,
            emptyMessage: "The burndown starts with the sprint.",
            formatValue: { "\($0.formatted()) pts" },
            onRetry: { status = .ready }
        )

        CatalogueCaption("A finished sprint with labelled days")
        MonarchBurndownChart(
            "Sprint 13",
            points: [30, 28, 24, 21, 18, 14, 11, 7, 3, 0].enumerated().map {
                MonarchBurndownChartPoint(label: "Day \($0.offset + 1)", remaining: Double($0.element))
            },
            height: 180
        )
    }
}
