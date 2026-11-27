import MonarchUI
import SwiftUI

struct MonarchEmptyStateSection: CataloguePage {
    static let title = "EmptyState"

    @State private var created = 0
    @State private var isFiltered = true

    var body: some View {
        CatalogueCaption("First run")
        MonarchEmptyState(
            "No invoices yet",
            message: "Invoices you send appear here. Created \(created).",
            systemImage: "doc.text",
            tip: "You can also import a CSV file.",
            actionTitle: "New invoice",
            secondaryActionTitle: "Import",
            action: { created += 1 },
            secondaryAction: { created += 10 }
        )
        CatalogueCaption("Filtered")
        if isFiltered {
            MonarchEmptyState(
                "No results",
                message: "No tickets match the filters.",
                kind: .filtered,
                onClearFilters: { isFiltered = false }
            )
        } else {
            MonarchButton("Filter again", variant: .ghost, size: .sm) { isFiltered = true }
        }
        CatalogueCaption("Success, small")
        MonarchEmptyState("All caught up", message: "Nothing needs your attention.", kind: .success, size: .sm)
    }
}
