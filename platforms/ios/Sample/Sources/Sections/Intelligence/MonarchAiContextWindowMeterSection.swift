import MonarchUI
import SwiftUI

struct MonarchAiContextWindowMeterSection: CataloguePage {
    static let title = "AI context window meter"

    @State private var used = 110_000
    @State private var isSummarizing = false

    var body: some View {
        CatalogueCaption("What fills the window, with summarize")
        MonarchAiContextWindowMeter(
            used: used,
            limit: 128_000,
            breakdown: [
                MonarchAiContextWindowPart("Instructions", tokens: 3_000),
                MonarchAiContextWindowPart("Attachments", tokens: 22_000),
                MonarchAiContextWindowPart("Messages", tokens: max(0, used - 25_000)),
            ],
            isSummarizing: isSummarizing
        ) {
            isSummarizing = true
            Task {
                try? await Task.sleep(for: .seconds(1))
                used = 40_000
                isSummarizing = false
            }
        }
        Button("Add 10,000 tokens") { used += 10_000 }
            .buttonStyle(.monarch(.secondary, size: .sm))

        CatalogueCaption("An estimate, without a breakdown")
        MonarchAiContextWindowMeter(used: 45_000, limit: 128_000, isApproximate: true)

        CatalogueCaption("Compact, for a toolbar")
        MonarchAiContextWindowMeter(
            usage: MonarchAiUsage(inputTokens: 100_000, outputTokens: 4_000),
            limit: 128_000,
            isCompact: true
        )
    }
}
