@_spi(Experimental) import MonarchUI
import SwiftUI

/// Recipe: the record block for linked docs as MonarchLinkedDocsPanel, behind
/// a `DisclosureGroup` that loads the docs when opened, or as a row that
/// pushes its own screen on iPhone.
struct DocsPanelSection: CataloguePage {
    static let title = "Docs panel (linked docs recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var items: [MonarchLinkedDocsPanelItem]?
    @State private var lastAction = "No action yet."

    private let loaded = [
        MonarchLinkedDocsPanelItem(id: "l1", title: "Release runbook", docID: "d1", meta: "Updated 2 hours ago"),
        MonarchLinkedDocsPanelItem(
            id: "l2",
            title: "Design file",
            kind: .link,
            url: URL(string: "https://design.example.com/f/1")
        ),
    ]

    var body: some View {
        Text(lastAction).monarchTextStyle(.body)

        CatalogueCaption("Behind a disclosure, loaded when opened")
        DisclosureGroup("Docs, 2 linked") {
            MonarchLinkedDocsPanel(
                items,
                onUnlink: { item in items?.removeAll { $0.id == item.id } },
                onOpen: { lastAction = "Opened \($0.title)" }
            )
            .task {
                guard items == nil else { return }
                try? await Task.sleep(for: .seconds(1))
                items = loaded
            }
        }
        .disclosureGroupStyle(.monarch)

        CatalogueCaption("A row of a record on iPhone")
        NavigationStack {
            List {
                NavigationLink {
                    ScrollView {
                        MonarchLinkedDocsPanel(loaded, isReadOnly: true, onOpen: { lastAction = "Opened \($0.title)" })
                            .padding(theme.spacing.space4)
                    }
                    .navigationTitle("Docs")
                } label: {
                    LabeledContent("Docs", value: "2")
                }
            }
        }
        .frame(minHeight: theme.spacing.space24 * 2)
    }
}
