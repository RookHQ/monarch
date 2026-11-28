import MonarchUI
import SwiftUI

struct MonarchMessageComposerSection: CataloguePage {
    static let title = "Message composer"

    private static let people = [
        MonarchMentionItem(id: "u1", label: "Ana Lopez", detail: "Design"),
        MonarchMentionItem(id: "u2", label: "Ben Okafor", detail: "Engineering"),
        MonarchMentionItem(id: "u3", label: "Chloé Martin", detail: "Support"),
    ]

    @State private var text = ""
    @State private var sent: [String] = []
    @State private var query: MonarchMentionQuery?
    @State private var replying = true
    @State private var files = ["brief.pdf"]
    @State private var ticketText = ""
    @State private var mode = "reply"
    @State private var editText = "Shipping on Friday."

    private var suggestions: [MonarchMentionItem] {
        guard let query else { return [] }
        return Self.people.filter { query.text.isEmpty || $0.label.localizedCaseInsensitiveContains(query.text) }
    }

    var body: some View {
        CatalogueCaption("Chat: type @ to mention someone")
        ForEach(sent, id: \.self) { message in
            Text(message).monarchTextStyle(.body)
        }
        MonarchMessageComposer(
            "Message #design",
            text: $text,
            placeholder: "Message #design",
            replyTo: replying
                ? MonarchMessageComposerReply(author: "Ana Lopez", excerpt: "Can we ship on Friday?") : nil,
            mentions: MonarchMessageComposerMentions(suggestions) { query = $0 },
            hasAttachments: !files.isEmpty,
            onAttach: { files.append("photo-\(files.count + 1).jpg") },
            onDismissReply: { replying = false },
            attachments: {
                ForEach(files, id: \.self) { file in
                    MonarchAttachmentChip(file, size: 240_000, onRemove: { files.removeAll { $0 == file } })
                }
            },
            onSend: { message in
                sent.append(message.text.isEmpty ? "\(files.count) files" : message.text)
                files = []
                replying = false
            }
        )
        CatalogueCaption("Ticket reply with an internal note mode")
        MonarchMessageComposer(
            "Ticket reply",
            text: $ticketText,
            modes: [
                MonarchMessageComposerMode("reply", label: "Reply", placeholder: "Reply to the customer"),
                MonarchMessageComposerMode(
                    "note",
                    label: "Internal note",
                    isNote: true,
                    placeholder: "Only your team sees this",
                    sendLabel: "Add note"
                ),
            ],
            selectedMode: $mode,
            sendShortcut: .commandReturn,
            maxLength: 500,
            showsEmojiButton: false
        ) { _ in }
        CatalogueCaption("Editing")
        MonarchMessageComposer("Edit message", text: $editText, isEditing: true, onCancelEdit: {}, onSend: { _ in })
        CatalogueCaption("Disabled")
        MonarchMessageComposer("Message", text: .constant(""), disabledReason: "Only admins can post here.") { _ in }
            .disabled(true)
    }
}
