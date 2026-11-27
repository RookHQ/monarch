import MonarchUI
import SwiftUI

struct MonarchSearchResultItemSection: CataloguePage {
    static let title = "Search result item"

    @State private var query = "travel receipt"
    @State private var opened = "None"

    var body: some View {
        TextField("Query", text: $query)
            .textFieldStyle(.roundedBorder)
        let terms = MonarchSearchResultItem.terms(from: query)
        CatalogueCaption("Interactive, with a file icon and meta")
        MonarchSearchResultItem(
            "Travel policy",
            path: ["Handbook", "Finance", "Travel"],
            snippet: "Book travel at least two weeks ahead and keep every receipt for the claim.",
            terms: terms,
            fileName: "travel-policy.pdf",
            action: { opened = "Travel policy" },
            meta: { MonarchBadge("PDF") }
        )
        MonarchSearchResultItem(
            "Expense receipts",
            path: ["Handbook", "Finance"],
            snippet: "Upload a photo of each receipt within 30 days of travel.",
            terms: terms,
            systemImage: "doc.text",
            action: { opened = "Expense receipts" },
            meta: { Text("2 days ago") }
        )
        Text("Opened: \(opened)").monarchTextStyle(.caption)
        CatalogueCaption("Highlights from the search service")
        MonarchSearchResultItem(
            "Roadmap",
            snippet: "The <mark>roadmap</mark> for the next quarter, with <b>markup</b> kept as text.",
            snippetHasMarkTags: true,
            systemImage: "map"
        )
        CatalogueCaption("In a list")
        List {
            MonarchSearchResultItem("Design review", path: ["Meetings"], terms: terms, systemImage: "person.2")
            MonarchSearchResultItem("Travel checklist", path: ["Handbook"], terms: terms, fileName: "checklist.xlsx")
        }
        .frame(minHeight: 200)
    }
}
