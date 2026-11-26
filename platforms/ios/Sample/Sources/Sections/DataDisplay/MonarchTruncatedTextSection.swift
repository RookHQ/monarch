import MonarchUI
import SwiftUI

struct MonarchTruncatedTextSection: CataloguePage {
    static let title = "TruncatedText"

    @State private var isExpanded = false

    private let note =
        "The supplier confirmed the delivery for Thursday, but the warehouse is closed that day, "
        + "so the driver needs the side entrance code and a contact number. Call the site manager "
        + "before noon to arrange access, and update the order once the pallets are signed for."

    var body: some View {
        CatalogueCaption("Three lines, keeps its own state")
        MonarchTruncatedText(note)
        CatalogueCaption("Two lines, state held by the page")
        MonarchTruncatedText(note, lineLimit: 2, isExpanded: $isExpanded)
        Toggle("Expanded", isOn: $isExpanded)
            .monarchTextStyle(.body)
        CatalogueCaption("One line, no button")
        MonarchTruncatedText(note, lineLimit: 1, isExpandable: false)
        CatalogueCaption("Short text: no button")
        MonarchTruncatedText("Delivered on Thursday.")
    }
}
