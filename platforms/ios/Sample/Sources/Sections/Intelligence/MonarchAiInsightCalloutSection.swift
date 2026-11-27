import MonarchUI
import SwiftUI

struct MonarchAiInsightCalloutSection: CataloguePage {
    static let title = "AI insight callout"

    @State private var isShown = true
    @State private var lastAction = ""

    var body: some View {
        CatalogueCaption("AI insight with evidence, an action and dismiss")
        if isShown {
            MonarchAiInsightCallout(
                "Renewal at risk",
                why: "Usage fell 40% in the last 30 days and two admins left the account.",
                origin: .ai,
                tone: .warning,
                actionTitle: "Plan outreach",
                action: { lastAction = "Outreach planned" },
                onDismiss: { isShown = false },
                evidence: {
                    VStack(alignment: .leading) {
                        Text("Weekly active users: 120 → 72")
                        Text("Admins: 3 → 1")
                    }
                    .monarchTextStyle(.caption)
                }
            )
        } else {
            Button("Show the insight again") { isShown = true }
                .buttonStyle(.monarch(.secondary, size: .sm))
        }
        if !lastAction.isEmpty {
            Text(lastAction).monarchTextStyle(.caption)
        }

        CatalogueCaption("Rule-based")
        MonarchAiInsightCallout(
            "Invoice is 30 days overdue",
            why: "Invoice 1042 was due on 1 October and is unpaid.",
            origin: .rule,
            tone: .danger
        )

        CatalogueCaption("Tones")
        MonarchAiInsightCallout(
            "Three renewals came in early",
            why: "All three followed the new onboarding.",
            origin: .ai,
            tone: .success
        )
        MonarchAiInsightCallout(
            "Quarter closes in 5 days",
            why: "Two deals are still in review.",
            origin: .rule,
            tone: .info
        )
    }
}
