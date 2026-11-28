import MonarchUI
import SwiftUI

struct MonarchAiUserMessageEditSection: CataloguePage {
    static let title = "AI user message edit"

    @Environment(\.monarchTheme) private var theme
    @State private var message = "Summarise the open risks from the review."
    @State private var isEditing = false

    var body: some View {
        CatalogueCaption("Edit a sent prompt")
        if isEditing {
            MonarchAiUserMessageEdit(
                message,
                onCancel: { isEditing = false },
                onSubmit: { text in
                    message = text
                    isEditing = false
                }
            )
        } else {
            Text(message).monarchTextStyle(.body)
            MonarchButton("Edit", systemImage: "pencil", size: .sm) {
                isEditing = true
            }
        }

        CatalogueCaption("Three lines at most, own strings")
        MonarchAiUserMessageEdit(
            "Compare the quarterly revenue with the forecast and list every gap.",
            maxLines: 3,
            branchHint: "Your first question is kept.",
            sendLabel: "Ask again",
            focusesOnAppear: false,
            onCancel: {},
            onSubmit: { _ in }
        )

        CatalogueCaption("Disabled while a reply streams")
        MonarchAiUserMessageEdit("Waiting for the reply", focusesOnAppear: false, onCancel: {}, onSubmit: { _ in })
            .disabled(true)
    }
}
