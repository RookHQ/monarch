import MonarchUI
import SwiftUI

struct MonarchFunnelChartSection: CataloguePage {
    static let title = "FunnelChart"

    @State private var alignment = MonarchFunnelChartAlignment.center
    @State private var showsSteps = true
    @State private var status = MonarchChartFrameStatus.ready
    @State private var selected = ""

    private let stages = [
        MonarchFunnelChartStage("Visited pricing", value: 4_800),
        MonarchFunnelChartStage("Started sign-up", value: 1_900),
        MonarchFunnelChartStage("Verified email", value: 1_200),
        MonarchFunnelChartStage("Added a card", value: 520),
        MonarchFunnelChartStage("Became customer", value: 310),
    ]

    var body: some View {
        Picker("Alignment", selection: $alignment) {
            Text("Centre").tag(MonarchFunnelChartAlignment.center)
            Text("Leading").tag(MonarchFunnelChartAlignment.leading)
        }
        .pickerStyle(.segmented)
        Toggle("Step lines", isOn: $showsSteps)
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchChartFrameStatus.ready)
            Text("Loading").tag(MonarchChartFrameStatus.loading)
            Text("Empty").tag(MonarchChartFrameStatus.empty)
            Text("Error").tag(MonarchChartFrameStatus.error)
        }
        .pickerStyle(.segmented)
        MonarchFunnelChart(
            "Sign-up funnel",
            stages: stages,
            description: "Last 30 days",
            status: status,
            alignment: alignment,
            showsSteps: showsSteps,
            emptyMessage: "Stages appear after the first visit.",
            footer: "Source: product analytics.",
            onRetry: { status = .ready },
            onSelect: { selected = $0.label }
        )
        if !selected.isEmpty {
            CatalogueCaption("Selected: \(selected)")
        }
    }
}
