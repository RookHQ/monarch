import MonarchUI
import SwiftUI

struct MonarchTypeToConfirmInputSection: CataloguePage {
    static let title = "Type to confirm"

    @State private var typed = ""
    @State private var didTry = false
    @State private var deleted = false

    private let phrase = "acme-site"

    var body: some View {
        MonarchTypeToConfirmInput(
            phrase: phrase,
            text: $typed,
            helper: "This deletes the site and its pages.",
            showsMismatch: didTry
        )
        MonarchButton("Delete site", variant: .danger) {
            didTry = true
            deleted = MonarchTypeToConfirmInput.matches(typed, phrase: phrase)
        }
        if deleted {
            MonarchBanner(message: "The text matched, so the action would run.", tone: .success)
        }

        CatalogueCaption("Custom label, disabled")
        MonarchTypeToConfirmInput(phrase: "DELETE", text: .constant(""), label: "Type DELETE to remove the workspace")
            .disabled(true)
    }
}
