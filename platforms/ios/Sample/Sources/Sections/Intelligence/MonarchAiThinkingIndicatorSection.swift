import MonarchUI
import SwiftUI

struct MonarchAiThinkingIndicatorSection: CataloguePage {
    static let title = "AI thinking indicator"

    @State private var isThinking = false
    @State private var startedAt = Date()

    var body: some View {
        CatalogueCaption("Shimmer (default). The elapsed time appears after 3 seconds.")
        Toggle("Assistant is thinking", isOn: $isThinking)
            .monarchTextStyle(.body)
            .onChange(of: isThinking) { startedAt = Date() }
        if isThinking {
            MonarchAiThinkingIndicator(startedAt: startedAt)
        }

        CatalogueCaption("Mark, with a named step, started 65 seconds ago")
        MonarchAiThinkingIndicator(
            label: "Searching your files…",
            startedAt: Date(timeIntervalSinceNow: -65),
            variant: .mark,
            isAnnounced: false
        )
    }
}
