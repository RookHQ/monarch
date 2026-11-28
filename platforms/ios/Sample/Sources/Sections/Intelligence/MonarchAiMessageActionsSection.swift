import MonarchUI
import SwiftUI

struct MonarchAiMessageActionsSection: CataloguePage {
    static let title = "AI message actions"

    @Environment(\.monarchTheme) private var theme
    @State private var feedback: MonarchAiMessageFeedback?
    @State private var isReading = false
    @State private var last = "Nothing yet"

    private let answer = "Paris is the capital of France. It has been since the 10th century."

    var body: some View {
        CatalogueCaption("Under an assistant message")
        Text(answer).monarchTextStyle(.body)
        MonarchAiMessageActions(
            answer,
            feedback: $feedback,
            isReadingAloud: $isReading,
            isShareable: true,
            onCopy: { _ in last = "Copied" },
            onRegenerate: { last = "Regenerate" },
            onEdit: { last = "Edit" },
            more: {
                Button("Report", systemImage: "flag") { last = "Report" }
            }
        )
        Text("Last action: \(last). Reading aloud: \(isReading ? "yes" : "no").")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Narrow: the rest moves into More")
        MonarchAiMessageActions(answer, feedback: $feedback, isShareable: true, onRegenerate: {}, onEdit: {})
            .frame(width: 200, alignment: .leading)

        CatalogueCaption("Your own message")
        MonarchAiMessageActions("Where is the Louvre?", onEdit: { last = "Edit" })
    }
}
