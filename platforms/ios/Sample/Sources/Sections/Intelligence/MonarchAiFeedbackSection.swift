import MonarchUI
import SwiftUI

struct MonarchAiFeedbackSection: CataloguePage {
    static let title = "AI feedback"

    @State private var rating: MonarchAiMessageFeedback?
    @State private var bothRating: MonarchAiMessageFeedback?
    @State private var failingRating: MonarchAiMessageFeedback?
    @State private var sent = "Nothing sent yet"

    private struct SendFailed: Error {}

    var body: some View {
        CatalogueCaption("Details after a thumbs down")
        MonarchAiFeedback(selection: $rating) { submission in
            sent = "\(submission.value.rawValue): \(submission.reasons.joined(separator: ", ")) \(submission.comment)"
        }
        Text(sent).monarchTextStyle(.caption)

        CatalogueCaption("Details after both")
        MonarchAiFeedback(
            selection: $bothRating,
            reasons: [
                .up: [
                    MonarchCheckboxGroupOption(id: "accurate", label: "Accurate"),
                    MonarchCheckboxGroupOption(id: "clear", label: "Clear"),
                ],
                .down: [MonarchCheckboxGroupOption(id: "wrong", label: "Wrong")],
            ],
            asksDetails: .both
        )

        CatalogueCaption("Sending fails")
        MonarchAiFeedback(selection: $failingRating, asksDetails: .none) { _ in
            throw SendFailed()
        }
    }
}
