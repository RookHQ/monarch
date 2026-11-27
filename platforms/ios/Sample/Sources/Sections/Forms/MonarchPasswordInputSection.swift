import MonarchUI
import SwiftUI

struct MonarchPasswordInputSection: CataloguePage {
    static let title = "Password input"

    @State private var current = ""
    @State private var new = ""
    @State private var isRevealed = false

    var body: some View {
        MonarchPasswordInput("Password", text: $current, prompt: "Your password")

        CatalogueCaption("New password, with the reveal state held by the page")
        MonarchPasswordInput(
            "New password",
            text: $new,
            helper: "At least 12 characters.",
            error: new.isEmpty || new.count >= 12 ? nil : "Use at least 12 characters.",
            purpose: .new,
            isRevealed: $isRevealed
        )
        Toggle("Show the new password", isOn: $isRevealed)
            .monarchTextStyle(.body)

        CatalogueCaption("Disabled")
        MonarchPasswordInput("Password", text: .constant("secret"))
            .disabled(true)
    }
}
