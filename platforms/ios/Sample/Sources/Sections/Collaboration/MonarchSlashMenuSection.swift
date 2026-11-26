import MonarchUI
import SwiftUI

struct MonarchSlashMenuSection: CataloguePage {
    static let title = "Slash menu"

    @State private var text = "Meeting notes\n/"
    @State private var query: MonarchSlashMenuQuery?
    @State private var isWordStart = false

    private let commands = [
        MonarchSlashMenuItem(
            id: "heading",
            label: "Heading",
            description: "Large section title",
            systemImage: "textformat.size",
            group: "Text",
            keywords: ["title", "h2"]
        ),
        MonarchSlashMenuItem(
            id: "quote",
            label: "Quote",
            description: "Indented quotation",
            systemImage: "text.quote",
            group: "Text"
        ),
        MonarchSlashMenuItem(
            id: "bullets",
            label: "Bulleted list",
            description: "A simple list",
            systemImage: "list.bullet",
            group: "Lists"
        ),
        MonarchSlashMenuItem(
            id: "checklist",
            label: "Checklist",
            description: "Items to tick off",
            systemImage: "checklist",
            group: "Lists",
            keywords: ["todo", "task"]
        ),
        MonarchSlashMenuItem(
            id: "table",
            label: "Table",
            description: "Not available in replies",
            systemImage: "tablecells",
            group: "Insert",
            isDisabled: true
        ),
    ]

    private let markdown = ["heading": "## ", "quote": "> ", "bullets": "- ", "checklist": "- [ ] "]

    var body: some View {
        CatalogueCaption("Options")
        Toggle("Open after a space too", isOn: $isWordStart)
            .monarchTextStyle(.body)

        CatalogueCaption("Type / at the start of a line; the menu shows above the field")
        VStack(spacing: 0) {
            MonarchSlashMenu(query, items: commands, groupOrder: ["Text", "Lists", "Insert"]) { item in
                guard let query else { return }
                text = query.replacing(in: text, with: markdown[item.id] ?? "")
                self.query = nil
            }
            TextField("Notes", text: $text, axis: .vertical)
                .textFieldStyle(.roundedBorder)
        }
        .onChange(of: text, initial: true) { _, new in
            query = MonarchSlashMenuQuery.find(in: new, position: isWordStart ? .wordStart : .lineStart)
        }
        .onChange(of: isWordStart) { _, wordStart in
            query = MonarchSlashMenuQuery.find(in: text, position: wordStart ? .wordStart : .lineStart)
        }
    }
}
