import MonarchUI
import SwiftUI

struct MonarchSecretFieldSection: CataloguePage {
    static let title = "Secret field"

    @State private var apiKey = "sk_live_51HxQ2JkL0mN"
    @State private var token = ""
    @State private var isRevealed = false
    @State private var copies = 0

    var body: some View {
        MonarchSecretField(
            "API key",
            text: $apiKey,
            helper: "Copies: \(copies)",
            isCopyable: true,
            onCopy: { _ in copies += 1 }
        )

        CatalogueCaption("Saved on the server, not shown")
        MonarchSecretField(
            "Bot token",
            text: $token,
            helper: token.isEmpty ? "Stored encrypted." : "The new token replaces the saved one when you save.",
            isSet: true
        )

        CatalogueCaption("Shown once, read-only, reveal state held by the page")
        MonarchSecretField(
            "Signing secret",
            text: .constant("whsec_9d2f7a61c0b84e3f"),
            helper: "Store it somewhere safe before you leave this page.",
            isCopyable: true,
            isReadOnly: true,
            isRevealed: $isRevealed
        )
        Toggle("Show the signing secret", isOn: $isRevealed)
            .monarchTextStyle(.body)

        CatalogueCaption("Error and disabled")
        MonarchSecretField("Token", text: .constant("abc"), error: "This token has expired.")
        MonarchSecretField("API key", text: .constant("secret"), isCopyable: true)
            .disabled(true)
    }
}
