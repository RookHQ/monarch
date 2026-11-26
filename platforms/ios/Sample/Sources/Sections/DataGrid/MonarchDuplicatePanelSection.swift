import MonarchUI
import SwiftUI

struct MonarchDuplicatePanelSection: CataloguePage {
    static let title = "DuplicatePanel"

    private static let all = [
        MonarchDuplicateMatch(
            id: "1",
            title: "Example Ltd",
            kind: "Client",
            reasons: ["Same email", "Same company domain"],
            detail: "Created 3 Mar",
            score: 0.92
        ),
        MonarchDuplicateMatch(id: "2", title: "Example Limited", kind: "Open lead", reasons: ["Same name"], score: 0.6),
    ]

    @State private var matches = MonarchDuplicatePanelSection.all
    @State private var lastAction = "No action yet"

    var body: some View {
        CatalogueCaption("Hint, under a create form")
        MonarchDuplicatePanel(
            matches,
            footer: "You can still create it.",
            onOpen: { lastAction = "Opened \($0.title)" },
            onMerge: { lastAction = "Merged \($0.title)" },
            onDismiss: { match in
                matches.removeAll { $0.id == match.id }
                lastAction = "Dismissed \(match.title)"
            }
        )
        Text(lastAction).monarchTextStyle(.caption)
        MonarchButton("Reset", variant: .ghost, size: .sm) {
            matches = MonarchDuplicatePanelSection.all
        }
        CatalogueCaption("Panel, on a record screen")
        MonarchDuplicatePanel(
            MonarchDuplicatePanelSection.all,
            variant: .panel,
            onOpen: { lastAction = "Opened \($0.title)" }
        )
    }
}
