import MonarchUI
import SwiftUI

struct MonarchWidgetSection: CataloguePage {
    static let title = "Widget"

    private enum Shown: String, CaseIterable {
        case ready, loading, empty, failed
    }

    @State private var shown: Shown = .ready
    @State private var isRetrying = false
    @State private var range = "30d"
    @State private var updatedAt = Date(timeIntervalSinceNow: -300)
    @State private var opened = 0

    private var state: MonarchWidgetState {
        switch shown {
        case .ready: .ready
        case .loading: .loading
        case .empty: .empty()
        case .failed: .failed()
        }
    }

    var body: some View {
        Picker("State", selection: $shown) {
            ForEach(Shown.allCases, id: \.self) { Text($0.rawValue).tag($0) }
        }
        .pickerStyle(.segmented)
        MonarchWidget(
            "Team load",
            description: "Open tickets across the support team",
            period: range == "30d" ? "Last 30 days" : "Last 7 days",
            systemImage: "person.3",
            state: state,
            isRetrying: isRetrying,
            updatedAt: updatedAt,
            onRetry: retry,
            onViewAll: { opened += 1 },
            actions: {
                Picker("Range", selection: $range) {
                    Text("7d").tag("7d")
                    Text("30d").tag("30d")
                }
                .pickerStyle(.segmented)
                .fixedSize()
            },
            menu: {
                Button("Refresh", systemImage: "arrow.clockwise") { updatedAt = .now }
                Divider()
                Button("Remove from dashboard", systemImage: "trash", role: .destructive) {}
            },
            content: {
                MonarchMetric(
                    "Open tickets",
                    value: range == "30d" ? 128 : 41,
                    change: MonarchMetricChange(0.12, comparison: "vs previous period"),
                    size: .lg
                )
            }
        )
        if opened > 0 {
            CatalogueCaption("View all pressed \(opened) times")
        }
        CatalogueCaption("Plain, inside another surface")
        MonarchWidget("Mentions", variant: .plain, state: .empty("No mentions this week")) {
            EmptyView()
        }
    }

    private func retry() {
        isRetrying = true
        Task {
            try? await Task.sleep(for: .seconds(1))
            isRetrying = false
            shown = .ready
        }
    }
}
