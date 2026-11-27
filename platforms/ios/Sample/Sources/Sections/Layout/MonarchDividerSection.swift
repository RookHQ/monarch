import MonarchUI
import SwiftUI

struct MonarchDividerSection: CataloguePage {
    static let title = "Divider"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text("Above").monarchTextStyle(.body)
        MonarchDivider()
        Text("Below").monarchTextStyle(.body)

        CatalogueCaption("In a horizontal stack")
        HStack(spacing: theme.spacing.space3) {
            Text("Before").monarchTextStyle(.body)
            MonarchDivider()
            Text("After").monarchTextStyle(.body)
        }
        .fixedSize(horizontal: false, vertical: true)
    }
}
