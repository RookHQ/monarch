import MonarchUI
import SwiftUI

struct MonarchAiMentionContextSection: CataloguePage {
    static let title = "AI mention context"

    @State private var prompt = "Summarise @q"
    @State private var context: [MonarchAiContextItem] = []

    private let catalog = [
        MonarchAiMentionItem(
            id: "d1",
            kind: .doc,
            label: "Q3 report.pdf",
            description: "Finance / Reports",
            tokens: 12_400
        ),
        MonarchAiMentionItem(id: "d2", kind: .doc, label: "Q3 board notes", description: "Board", tokens: 3_100),
        MonarchAiMentionItem(id: "p1", kind: .person, label: "Quinn Adams", description: "Finance lead"),
        MonarchAiMentionItem(
            id: "r1",
            kind: .record,
            label: "Quarterly forecast",
            description: "Forecast",
            tokens: 900
        ),
        MonarchAiMentionItem(id: "a1", kind: .app, label: "QuickBooks", description: "No access", isDisabled: true),
    ]

    private var query: MonarchMentionQuery? { MonarchMentionQuery.find(in: prompt) }

    private var results: [MonarchAiMentionItem] {
        let text = query?.text.lowercased() ?? ""
        let chosen = Set(context.map(\.id))
        return catalog.filter { !chosen.contains($0.id) && (text.isEmpty || $0.label.lowercased().contains(text)) }
    }

    var body: some View {
        CatalogueCaption("Type after @ to search")
        TextField("Prompt", text: $prompt)
            .textFieldStyle(.roundedBorder)
        MonarchAiMentionContext(query, items: results) { item in
            context.append(item.contextItem)
            if let query { prompt = query.replacing(in: prompt, with: "") }
        }
        MonarchAiContextChips(context) { id in
            context.removeAll { $0.id == id }
        }

        CatalogueCaption("Searching and failed")
        MonarchAiMentionContext(MonarchMentionQuery.find(in: "@q"), items: [], status: .loading) { _ in }
        MonarchAiMentionContext(
            MonarchMentionQuery.find(in: "@q"),
            items: [],
            status: .failed,
            onRetry: {},
            onSelect: { _ in }
        )
    }
}
