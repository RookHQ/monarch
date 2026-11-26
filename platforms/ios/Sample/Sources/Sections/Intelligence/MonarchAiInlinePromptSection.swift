import MonarchUI
import SwiftUI

/// A selection, wrapped so `.sheet(item:)` can present it.
private struct AskedSelection: Identifiable {
    let id = UUID()
    let text: String
}

struct MonarchAiInlinePromptSection: CataloguePage {
    static let title = "AiInlinePrompt"

    @Environment(\.monarchTheme) private var theme
    @State private var draft = "Thanks for the update. I will send the signed contract today, after the review."
    @State private var asked: AskedSelection?
    @State private var isAskingParagraph = false
    @State private var isPending = false
    @State private var lastPrompt = "Nothing sent yet"

    private let paragraph = "Early numbers suggest more people finish setup on their first visit."

    var body: some View {
        CatalogueCaption("Select text, then choose Ask AI in the text menu")
        MonarchAiInlinePromptTextView("Draft", text: $draft) { selection in
            asked = AskedSelection(text: selection)
        }
        .sheet(item: $asked) { selection in
            MonarchAiInlinePrompt(
                context: selection.text,
                suggestions: ["Shorten", "Make it more formal", "Fix spelling"]
            ) { prompt in
                lastPrompt = prompt
            }
        }

        CatalogueCaption("Long press the paragraph for Ask AI")
        MonarchAiInlinePromptArea(
            onAskAi: { isAskingParagraph = true },
            content: {
                Text(paragraph).monarchTextStyle(.body)
            }
        )
        .sheet(isPresented: $isAskingParagraph) {
            MonarchAiInlinePrompt(context: paragraph, closesOnSubmit: false, isPending: isPending) { prompt in
                lastPrompt = prompt
                isPending = true
                Task { @MainActor in
                    try? await Task.sleep(for: .seconds(1))
                    isPending = false
                }
            }
        }

        CatalogueCaption("A visible button opens the same sheet")
        MonarchButton("Edit paragraph with AI", systemImage: "text.bubble", size: .sm) {
            isAskingParagraph = true
        }
        Text("Last prompt: \(lastPrompt)")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
