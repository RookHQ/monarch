import MonarchUI
import SwiftUI

struct MonarchAiToolResultCardSection: CataloguePage {
    static let title = "AI tool result card"

    private struct UndoFailure: Error {}

    @State private var cardID = UUID()

    var body: some View {
        CatalogueCaption("A record created, with Undo that takes a moment")
        MonarchAiToolResultCard(
            "Acme Ltd",
            kind: .recordCreated,
            description: "Added to Accounts",
            url: URL(string: "https://example.com/accounts/42"),
            meta: [
                MonarchKeyValueItem("Owner", value: "Ana Flores"),
                MonarchKeyValueItem("Stage", value: "Lead"),
            ],
            onUndo: { try await Task.sleep(for: .seconds(1)) }
        )
        .id(cardID)
        MonarchButton("Reset", variant: .ghost, size: .sm) { cardID = UUID() }

        CatalogueCaption("An email sent, with Undo that fails")
        MonarchAiToolResultCard(
            "Renewal terms",
            kind: .emailSent,
            description: "To Ana Flores and 2 others",
            onUndo: { throw UndoFailure() }
        )

        CatalogueCaption("Event booked, ticket filed and generic")
        MonarchAiToolResultCard("Design review", kind: .eventBooked, description: "Tue 14 Oct, 10:00–10:30")
        MonarchAiToolResultCard(
            "Login fails on Safari",
            kind: .ticketFiled,
            meta: [MonarchKeyValueItem("ID", value: "SUP-812")]
        )
        MonarchAiToolResultCard("Synced 42 contacts")
    }
}
