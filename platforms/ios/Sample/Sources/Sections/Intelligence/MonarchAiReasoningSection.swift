import MonarchUI
import SwiftUI

struct MonarchAiReasoningSection: CataloguePage {
    static let title = "AI reasoning"

    private static let fullText =
        "The person asks for the cheapest flight to Lisbon in May. I should compare the three fares, then check the baggage rules, because the cheapest fare may not include a bag."

    @State private var part: MonarchAiReasoningPart?
    @State private var isRunning = false

    var body: some View {
        CatalogueCaption("Stream the reasoning; it closes when it finishes")
        if let part {
            MonarchAiReasoning(part)
        }
        MonarchButton(isRunning ? "Thinking" : "Ask", variant: .primary, size: .sm, isLoading: isRunning) {
            run()
        }

        CatalogueCaption("Finished, with a duration")
        MonarchAiReasoning(MonarchAiReasoningPart(text: Self.fullText, durationMs: 12_000))

        CatalogueCaption("Finished, open")
        MonarchAiReasoning(MonarchAiReasoningPart(text: Self.fullText, durationMs: 65_000), startsExpanded: true)

        CatalogueCaption("Redacted")
        MonarchAiReasoning(MonarchAiReasoningPart(text: "", durationMs: 4_000, isRedacted: true))
    }

    private func run() {
        isRunning = true
        part = MonarchAiReasoningPart(text: "", state: .streaming)
        Task {
            var text = ""
            for word in Self.fullText.split(separator: " ") {
                try? await Task.sleep(for: .milliseconds(120))
                text += (text.isEmpty ? "" : " ") + word
                part = MonarchAiReasoningPart(text: text, state: .streaming)
            }
            part = MonarchAiReasoningPart(text: text, state: .done)
            isRunning = false
        }
    }
}
