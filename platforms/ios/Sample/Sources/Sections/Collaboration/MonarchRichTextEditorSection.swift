@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchRichTextEditorSection: CataloguePage {
    static let title = "Rich text editor"

    var body: some View {
        if #available(iOS 26.0, *) {
            MonarchRichTextEditorDemo()
        } else {
            Text("The rich-text editor needs iOS 26.")
                .monarchTextStyle(.body)
        }
    }
}

@available(iOS 26.0, *)
private struct MonarchRichTextEditorDemo: View {
    @State private var notes = AttributedString("Release notes\nThe export now keeps formatting.")
    @State private var reply = AttributedString()
    @State private var mentionItems: [MonarchMentionItem] = []
    @State private var mentioned: [String] = []
    @State private var isReadOnly = false

    private let directory = [
        MonarchMentionItem(id: "u1", label: "Ana Lopez", detail: "Design"),
        MonarchMentionItem(id: "u2", label: "Ben Okafor", detail: "Engineering"),
        MonarchMentionItem(id: "c1", label: "announcements", kind: .channel, detail: "214 members"),
    ]

    var body: some View {
        CatalogueCaption("Options")
        Toggle("Read only", isOn: $isReadOnly)
            .monarchTextStyle(.body)

        CatalogueCaption("A document with the default toolbar")
        MonarchRichTextEditor("Release notes", text: $notes, prompt: "Write the notes", isReadOnly: isReadOnly)

        CatalogueCaption("A reply with a limit and mentions; type @")
        MonarchRichTextEditor(
            "Reply",
            text: $reply,
            prompt: "Reply to the thread",
            tools: MonarchEditorTool.inlineGroups,
            maxLength: 280,
            mentionItems: mentionItems,
            onMentionSearch: { query in
                mentionItems = search(query)
            },
            onMention: { mentioned.append($0.label) }
        )
        if !mentioned.isEmpty {
            Text("Will notify: \(mentioned.joined(separator: ", "))").monarchTextStyle(.caption)
        }
        Text(MonarchRichTextEditor.isOverLimit(reply, maxLength: 280) ? "Too long to send" : "Ready to send")
            .monarchTextStyle(.caption)
    }

    private func search(_ query: MonarchMentionQuery?) -> [MonarchMentionItem] {
        guard let query else { return [] }
        let kind: MonarchMentionKind = query.trigger == "#" ? .channel : .person
        return directory.filter {
            $0.kind == kind && (query.text.isEmpty || $0.label.localizedCaseInsensitiveContains(query.text))
        }
    }
}
