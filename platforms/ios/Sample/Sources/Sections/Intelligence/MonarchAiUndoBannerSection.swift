import MonarchUI
import SwiftUI

struct MonarchAiUndoBannerSection: CataloguePage {
    static let title = "AI undo banner"

    private struct UndoFailure: Error {}

    @State private var showsBanner = true
    @State private var bannerID = UUID()
    @State private var lastEvent = "No event yet."

    private let changes = [
        MonarchAiChange("Status", before: "Open", after: "Closed"),
        MonarchAiChange("Owner", before: "Ana Flores", after: "Ben Okafor"),
        MonarchAiChange("Priority", before: "Low", after: "High"),
        MonarchAiChange("Due date", before: "14 Oct", after: "21 Oct"),
    ]

    var body: some View {
        Text(lastEvent).monarchTextStyle(.body)

        CatalogueCaption("Two changes, an audit link and dismiss")
        if showsBanner {
            MonarchAiUndoBanner(
                "AI changed 2 fields",
                description: "On the Acme Ltd account",
                changes: Array(changes.prefix(2)),
                auditURL: URL(string: "https://example.com/audit/812"),
                onUndo: { try await Task.sleep(for: .seconds(1)) },
                onDismiss: { showsBanner = false }
            )
        } else {
            MonarchButton("Show the banner again", size: .sm) { showsBanner = true }
        }

        CatalogueCaption("Four changes, Undo for 10 seconds")
        MonarchAiUndoBanner(
            "AI changed 4 fields",
            changes: changes,
            duration: .seconds(10),
            onUndo: { lastEvent = "Undone" },
            onExpire: { lastEvent = "Undo expired" }
        )
        .id(bannerID)
        MonarchButton("Restart", variant: .ghost, size: .sm) { bannerID = UUID() }

        CatalogueCaption("Undo that fails")
        MonarchAiUndoBanner("AI archived 12 tickets", onUndo: { throw UndoFailure() })
    }
}
