import MonarchUI
import SwiftUI

struct MonarchAiFollowUpSuggestionsSection: CataloguePage {
    static let title = "AI follow-up suggestions"

    @State private var chosen = "Nothing chosen yet."
    @State private var isGenerating = false

    private let suggestions = [
        "What changed since last quarter?",
        "Show it as a table",
        "Which customers are at risk of leaving this month, and why?",
        "Draft an email to the team",
    ]

    var body: some View {
        Text(chosen)
            .monarchTextStyle(.body)
        MonarchAiFollowUpSuggestions(suggestions) { suggestion in
            chosen = "Sent: \(suggestion)"
        }
        .disabled(isGenerating)
        Toggle("Generating a reply", isOn: $isGenerating)
            .toggleStyle(.monarchCheckbox)
    }
}
