import MonarchUI
import SwiftUI

struct MonarchAiSourceListSection: CataloguePage {
    static let title = "AI source list"

    @State private var previewed: MonarchAiSourcePart?

    private let items: [MonarchAiSourceListItem] = [
        MonarchAiSourceListItem(
            MonarchAiSourcePart(
                id: "s1",
                title: "Quarterly planning notes",
                url: "https://www.example.com/notes/q3",
                kind: .doc,
                product: "Docs"
            ),
            snippet: "Revenue grew 12% against the same quarter last year."
        ),
        MonarchAiSourceListItem(MonarchAiSourcePart(id: "s2", title: "Acme Ltd", kind: .record, product: "CRM")),
        MonarchAiSourceListItem(
            MonarchAiSourcePart(id: "s3", title: "Industry report", url: "https://example.org/report", kind: .web)
        ),
        MonarchAiSourceListItem(MonarchAiSourcePart(id: "s4", title: "Old forecast.xlsx", kind: .file), isUsed: false),
        MonarchAiSourceListItem(
            MonarchAiSourcePart(id: "s5", title: "Board deck draft", kind: .doc, product: "Docs"),
            isUsed: false
        ),
    ]

    var body: some View {
        CatalogueCaption("Used and considered, collapsed after three; each opens a preview")
        MonarchAiSourceList(items) { item in
            previewed = item.source
        }
        .sheet(item: $previewed) { source in
            MonarchAiSourcePreview(
                source,
                quote: "revenue grew 12%",
                content: "Planning for the third quarter.\n\nRevenue grew 12% against the same quarter last year."
            )
        }

        CatalogueCaption("Used only, as links, without numbers")
        MonarchAiSourceList(Array(items.prefix(3)), isNumbered: false)
    }
}
