import MonarchUI
import SwiftUI

struct MonarchAiUsageMeterSection: CataloguePage {
    static let title = "AI usage meter"

    @State private var conversation: Double = 12_400

    var body: some View {
        CatalogueCaption("This turn, the conversation and this month")
        MonarchAiUsageMeter(
            scopes: [
                MonarchAiUsageScope("This turn", used: 1_850),
                MonarchAiUsageScope("Conversation", used: conversation, limit: 50_000),
                MonarchAiUsageScope("This month", used: 1_040, limit: 1_000, unit: .credits),
            ]
        )
        Button("Use 8,000 more tokens") { conversation += 8_000 }
            .buttonStyle(.monarch(.secondary, size: .sm))
        Button("Reset") { conversation = 12_400 }
            .buttonStyle(.monarch(.ghost, size: .sm))

        CatalogueCaption("Titled, with a period")
        MonarchAiUsageMeter(
            "AI usage",
            scopes: [MonarchAiUsageScope("Image generation", used: 32, limit: 100, unit: .credits)],
            period: "November"
        )
    }
}
