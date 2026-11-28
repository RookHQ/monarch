import MonarchUI
import SwiftUI

struct MonarchStatCardSection: CataloguePage {
    static let title = "Stat card"

    @State private var opened = "Nothing opened"
    @State private var isLoading = false
    @State private var isRetrying = false

    var body: some View {
        CatalogueCaption("Drill-down, help and sparkline")
        MonarchStatCard(
            "Open tickets",
            value: 128,
            change: MonarchMetricChange(0.12, comparison: "vs last week", goodDirection: .down),
            hint: "38 waiting on a customer",
            help: "Tickets that are not solved or closed.",
            systemImage: "tray",
            sparkline: [96, 104, 110, 121, 128],
            action: { opened = "Opened open tickets" }
        )
        Text(opened).monarchTextStyle(.caption)

        CatalogueCaption("Sizes")
        ForEach(MonarchMetricSize.allCases, id: \.self) { size in
            MonarchStatCard("Revenue", value: 1_284_000, format: .currency(code: "EUR"), isCompact: true, size: size)
        }

        CatalogueCaption("Tones")
        MonarchStatCard("Error rate", value: 0.042, format: .percent, hint: "Above the 2% target", tone: .danger)
        MonarchStatCard("Uptime", value: 0.9995, format: .percent, hint: "Within target", tone: .success)

        CatalogueCaption("Plain, inside another surface")
        MonarchCard {
            MonarchStatCard("Rating", text: "4.8 / 5", spokenText: "4.8 out of 5", variant: .plain)
        }

        CatalogueCaption("Loading, empty and error")
        Toggle("Loading", isOn: $isLoading)
        MonarchStatCard("Churn", value: 0.031, format: .percent, isLoading: isLoading)
        MonarchStatCard("Churn", value: nil)
        MonarchStatCard(
            "Uptime",
            value: nil,
            errorMessage: "Couldn’t load uptime.",
            isRetrying: isRetrying,
            onRetry: { isRetrying.toggle() }
        )
    }
}
