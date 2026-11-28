import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct UnlinkError: Error {}

struct MonarchLinkedDocsPanelSection: CataloguePage {
    static let title = "Linked docs panel"

    @State private var items = [
        MonarchLinkedDocsPanelItem(
            id: "1",
            title: "Launch plan",
            docID: "d1",
            meta: "Updated 2 hours ago",
            status: "Draft",
            spaceID: "mkt",
            spaceName: "Marketing"
        ),
        MonarchLinkedDocsPanelItem(id: "2", title: "Brand guide", docID: "d2", spaceID: "design", spaceName: "Design"),
        MonarchLinkedDocsPanelItem(
            id: "3",
            title: "Runbook",
            kind: .link,
            url: URL(string: "https://www.example.com/runbook")
        ),
    ]
    @State private var failsOnce = true
    @State private var opened = "Nothing opened"

    private let docs = [
        MonarchLinkedDocsPanelOption(docID: "d1", label: "Launch plan"),
        MonarchLinkedDocsPanelOption(docID: "d9", label: "Pricing notes", description: "Finance"),
        MonarchLinkedDocsPanelOption(docID: "d10", label: "Hiring plan", description: "People"),
    ]

    var body: some View {
        CatalogueCaption("The first unlink fails, the next one succeeds")
        MonarchLinkedDocsPanel(
            items,
            docOptions: docs,
            onLinkDoc: { docID in
                try await Task.sleep(for: .milliseconds(500))
                let label = docs.first { $0.docID == docID }?.label ?? docID
                items.append(MonarchLinkedDocsPanelItem(id: UUID().uuidString, title: label, docID: docID))
            },
            onCreateDoc: { opened = "New doc" },
            onAddLink: { url, title in
                try await Task.sleep(for: .milliseconds(500))
                items.append(MonarchLinkedDocsPanelItem(id: UUID().uuidString, title: title, kind: .link, url: url))
            },
            onUnlink: { item in
                try await Task.sleep(for: .milliseconds(600))
                if failsOnce {
                    failsOnce = false
                    throw UnlinkError()
                }
                items.removeAll { $0.id == item.id }
            },
            onOpen: { opened = "Opened \($0.title)" }
        )
        Text(opened).monarchTextStyle(.caption)

        CatalogueCaption("Loading, read only and notice")
        MonarchLinkedDocsPanel(nil, onOpen: { _ in })
        MonarchLinkedDocsPanel([], isReadOnly: true, onOpen: { _ in })
        MonarchLinkedDocsPanel(items, notice: "You don’t have access to docs in this workspace.", onOpen: { _ in })
    }
}
