@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiSummaryCardSection: CataloguePage {
    static let title = "AI summary card"

    @State private var status: MonarchAiSummaryCardStatus = .ready
    @State private var generatedAt = Date().addingTimeInterval(-3 * 3_600)

    var body: some View {
        CatalogueCaption("Pick a state; Refresh writes a new summary")
        Picker("State", selection: $status) {
            Text("Ready").tag(MonarchAiSummaryCardStatus.ready)
            Text("Generating").tag(MonarchAiSummaryCardStatus.generating)
            Text("Stale").tag(MonarchAiSummaryCardStatus.stale)
            Text("Error").tag(MonarchAiSummaryCardStatus.error)
        }
        .pickerStyle(.segmented)
        MonarchAiSummaryCard(
            "Account summary",
            summary: status == .error
                ? ""
                : "Renewal is **in 30 days**. Usage grew 12% this quarter.\n\n- Two open tickets\n- One overdue invoice",
            status: status,
            generatedAt: generatedAt,
            sources: [
                MonarchAiSourcePart(id: "s1", title: "Q3 usage report", url: "https://example.com/q3", kind: .doc),
                MonarchAiSourcePart(id: "s2", title: "Invoice 1042", kind: .record),
            ],
            errorMessage: "The model timed out.",
            onRefresh: {
                status = .ready
                generatedAt = Date()
            }
        )

        CatalogueCaption("The first summary is being written")
        MonarchAiSummaryCard("Deal summary", summary: "", status: .generating, onRefresh: {})
    }
}
