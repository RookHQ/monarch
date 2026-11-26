import MonarchUI
import SwiftUI

struct MonarchInsightStripSection: CataloguePage {
    static let title = "Insight strip"

    @State private var dismissed: Set<String> = []
    @State private var isLoading = false
    @State private var hasFailed = false
    @State private var reviewed = 0

    private var insights: [MonarchInsight] {
        [
            MonarchInsight(
                id: "usage",
                title: "Usage is up 12% this month",
                why: "More seats were active than in any month this year.",
                tone: .success,
                origin: .rule
            ),
            MonarchInsight(
                id: "renewals",
                title: "3 domains expire in 30 days",
                why: "Auto-renew is off for all three.",
                tone: .warning,
                origin: .rule,
                evidence: "example.com, example.org, example.net",
                actionTitle: "Review renewals",
                action: { reviewed += 1 }
            ),
            MonarchInsight(
                id: "churn",
                title: "Renewal at risk",
                why: "Usage fell by half in the last month.",
                tone: .danger,
                origin: .ai
            ),
        ]
        .filter { !dismissed.contains($0.id) }
    }

    var body: some View {
        MonarchInsightStrip(
            "Needs attention",
            insights: insights,
            isLoading: isLoading,
            error: hasFailed ? "The insights could not be loaded." : nil,
            onRetry: { hasFailed = false },
            onDismiss: { dismissed.insert($0) }
        )
        Toggle("Loading", isOn: $isLoading)
        Toggle("Failed", isOn: $hasFailed)
        Button("Restore dismissed insights") { dismissed = [] }
            .buttonStyle(.monarch(.ghost, size: .sm))
        Text("Review renewals activated \(reviewed) times").monarchTextStyle(.caption)

        CatalogueCaption("Stack, limited to one")
        MonarchInsightStrip(insights: insights, layout: .stack, limit: 1)
        CatalogueCaption("Grid")
        MonarchInsightStrip(insights: insights, layout: .grid)
        CatalogueCaption("Empty")
        MonarchInsightStrip(insights: [])
    }
}
