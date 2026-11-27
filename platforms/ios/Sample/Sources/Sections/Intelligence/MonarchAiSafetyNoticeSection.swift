import MonarchUI
import SwiftUI

struct MonarchAiSafetyNoticeSection: CataloguePage {
    static let title = "AI safety notice"

    @State private var lastAction = "No action yet."

    var body: some View {
        Text(lastAction)
            .monarchTextStyle(.body)

        CatalogueCaption("Blocked request with next steps")
        MonarchAiSafetyNotice(
            .blocked,
            reason: "The request asked for someone’s home address.",
            nextSteps: "Ask about public information instead, such as a company’s office location.",
            learnMoreURL: URL(string: "https://example.com/guidelines")
        ) {
            lastAction = "Edit prompt"
        }

        CatalogueCaption("Adjusted answer")
        MonarchAiSafetyNotice(.modified, reason: "Medical dosage details were removed.")

        CatalogueCaption("Filtered answer")
        MonarchAiSafetyNotice(.filtered)
    }
}
