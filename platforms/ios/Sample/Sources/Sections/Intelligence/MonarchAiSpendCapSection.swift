@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiSpendCapSection: CataloguePage {
    static let title = "AI spend cap"

    @State private var spent = 1_240
    @State private var settings = MonarchAiSpendCapSettings(limit: 5_000, warnAt: 0.8, behavior: .pause)
    @State private var requested = false

    var body: some View {
        MonarchAiSpendCap(
            spent: spent,
            limit: settings.limit,
            currencyCode: "GBP",
            period: .month,
            warnAt: settings.warnAt,
            behavior: settings.behavior,
            resetsAt: Calendar.current.date(byAdding: .day, value: 12, to: .now),
            breakdown: [
                MonarchAiSpendCapBreakdownItem(id: "research", label: "Research agent", spent: spent * 2 / 3),
                MonarchAiSpendCapBreakdownItem(id: "summaries", label: "Meeting summaries", spent: spent / 3),
            ],
            maxLimit: 100_000
        ) { newSettings in
            settings = newSettings
        }
        HStack {
            Button("Spend £10") { spent += 1_000 }
            Button("New period") { spent = 0 }
        }
        .buttonStyle(.monarch(.secondary, size: .sm))

        CatalogueCaption("Read only, at the limit")
        MonarchAiSpendCap(
            spent: 5_000,
            limit: 5_000,
            currencyCode: "GBP",
            period: .month,
            canEdit: false,
            onRequestIncrease: { requested = true }
        )
        if requested {
            Text("Request sent to the workspace admin.").monarchTextStyle(.caption)
        }
    }
}
