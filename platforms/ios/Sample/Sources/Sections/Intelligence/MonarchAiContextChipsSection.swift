import MonarchUI
import SwiftUI

struct MonarchAiContextChipsSection: CataloguePage {
    static let title = "AI context chips"

    private static let all = [
        MonarchAiContextItem(
            id: "contract",
            kind: .file,
            label: "Q3 contract.pdf",
            tokens: 12_400,
            description: "Shared/Legal/Q3"
        ),
        MonarchAiContextItem(id: "selection", kind: .selection, label: "Selected text", tokens: 820),
        MonarchAiContextItem(id: "page", kind: .page, label: "Invoices", tokens: 2_100, isRemovable: false),
        MonarchAiContextItem(id: "record", kind: .record, label: "Acme Corporation", tokens: 640),
        MonarchAiContextItem(id: "person", kind: .person, label: "Dana Whitfield"),
    ]

    @State private var items = Self.all

    var body: some View {
        CatalogueCaption("Remove chips; the page stays")
        MonarchAiContextChips(items, emptyText: "Nothing is attached.") { id in
            items.removeAll { $0.id == id }
        }
        MonarchButton("Reset", variant: .ghost, size: .sm) { items = Self.all }

        CatalogueCaption("Against a 128K budget, read only")
        MonarchAiContextChips(Self.all, budget: 128_000)
    }
}
