import MonarchUI
import SwiftUI

/// Native adoption: the standard `ShareLink`, which opens the system share
/// sheet, with the Monarch button style.
struct ShareButtonSection: CataloguePage {
    static let title = "ShareButton (standard ShareLink)"

    private let link = "https://example.com/invoices/1042"

    var body: some View {
        CatalogueCaption("Default label")
        ShareLink(item: link)
            .buttonStyle(.monarch())

        CatalogueCaption("Title, subject and message")
        ShareLink(
            "Share invoice",
            item: link,
            subject: Text("Invoice 1042"),
            message: Text("Here is the invoice.")
        )
        .buttonStyle(.monarch(.primary))

        CatalogueCaption("Icon only")
        ShareLink(item: link) {
            Label("Share invoice", systemImage: "square.and.arrow.up")
        }
        .labelStyle(.iconOnly)
        .buttonStyle(.monarch(.ghost))
    }
}
