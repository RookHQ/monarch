import MonarchUI
import SwiftUI

struct MonarchAiGhostTextSection: CataloguePage {
    static let title = "AI ghost text"

    @State private var message = ""
    @State private var note = "The meeting"
    @State private var completion = MonarchAiCompletion(minimumCharacters: 1) { text, _ in
        try await Task.sleep(for: .milliseconds(400))
        return text.hasSuffix(" ") ? "is moved to Thursday at ten." : " is moved to Thursday at ten."
    }
    @State private var lastEvent = "Nothing accepted yet."

    /// A scripted completion: no network, the same continuations every time.
    private static func complete(_ text: String, _ cursor: Int) async throws -> String? {
        try await Task.sleep(for: .milliseconds(400))
        let lower = text.lowercased()
        if lower.hasSuffix("thanks") { return " for getting back to me so quickly." }
        if lower.hasSuffix("i will") { return " send the report by Friday." }
        if lower.hasSuffix("best") { return " regards," }
        return nil
    }

    var body: some View {
        CatalogueCaption("Type \"Thanks\", \"I will\" or \"Best\", then tap the suggestion or press Tab")
        MonarchAiGhostText(
            "Message",
            text: $message,
            prompt: "Write a reply",
            helper: "Suggestions are scripted in the catalogue.",
            onAccept: { acceptance, kind in
                lastEvent = "Accepted \(kind == .word ? "a word" : "all"): \"\(acceptance.inserted)\""
            },
            onDismiss: { lastEvent = "Dismissed." },
            complete: Self.complete
        )
        Text(lastEvent)
            .monarchTextStyle(.caption)

        CatalogueCaption("With a completion the page keeps: \(completion.status.rawValue)")
        MonarchAiGhostText("Note", text: $note, completion: completion, showsAcceptButton: false)

        CatalogueCaption("Disabled")
        MonarchAiGhostText("Message", text: .constant("Read only")) { _, _ in nil }
            .disabled(true)
    }
}
