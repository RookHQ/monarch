@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiSuggestedActionsSection: CataloguePage {
    static let title = "AI suggested actions"

    private struct RunFailed: Error {}

    @State private var actions = [
        MonarchAiSuggestedAction(
            id: "remind",
            label: "Send the renewal reminder",
            description: "The contract ends in 14 days."
        ),
        MonarchAiSuggestedAction(
            id: "close",
            label: "Close 4 stale tickets",
            risk: .destructive,
            consequences: ["4 tickets will be closed", "Requesters are notified"],
            confirmLabel: "Close tickets"
        ),
        MonarchAiSuggestedAction(
            id: "fail",
            label: "Sync with the CRM",
            description: "This one fails, to show the failure line.",
            risk: .medium
        ),
    ]

    var body: some View {
        CatalogueCaption("Run, approval and dismiss")
        MonarchAiSuggestedActions(
            actions,
            onDismiss: { id in actions.removeAll { $0.id == id } },
            onRun: { id in
                try await Task.sleep(for: .seconds(1))
                if id == "fail" { throw RunFailed() }
            }
        )

        CatalogueCaption("Rule-based")
        MonarchAiSuggestedActions(
            [MonarchAiSuggestedAction(id: "call", label: "Call the customer back", status: .done)],
            title: "Next steps",
            origin: .rule
        ) { _ in }

        CatalogueCaption("No suggestions")
        MonarchAiSuggestedActions([]) { _ in }
    }
}
