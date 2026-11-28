import MonarchUI
import SwiftUI

struct MonarchUsageMeterSection: CataloguePage {
    static let title = "UsageMeter"

    @Environment(\.monarchTheme) private var theme
    @State private var requests = 4_200.0
    @State private var isLoading = false
    @State private var hasError = false

    var body: some View {
        MonarchUsageMeter(
            "Usage",
            items: [
                MonarchUsageMeterItem("Requests", used: requests, limit: 10_000),
                MonarchUsageMeterItem("Storage", used: 42, limit: 50, unit: "GB"),
                MonarchUsageMeterItem("Minutes", used: 130, limit: 100, description: "Extra minutes are billed."),
                MonarchUsageMeterItem("Seats", used: 4, unit: "seats"),
            ],
            period: "This month",
            resetsAt: Date(timeIntervalSinceNow: 60 * 60 * 24 * 12),
            isLoading: isLoading,
            error: hasError ? "Usage could not be loaded." : nil
        ) {
            hasError = false
        }

        CatalogueCaption("Change the state")
        HStack(spacing: theme.spacing.space2) {
            MonarchButton("Add 2,000 requests", size: .sm) { requests += 2_000 }
            MonarchButton("Reset", variant: .ghost, size: .sm) { requests = 4_200 }
        }
        Toggle("Loading", isOn: $isLoading)
            .monarchTextStyle(.body)
        Toggle("Error", isOn: $hasError)
            .monarchTextStyle(.body)
    }
}
