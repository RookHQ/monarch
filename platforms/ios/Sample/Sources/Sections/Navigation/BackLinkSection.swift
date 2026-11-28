import MonarchUI
import SwiftUI

/// Native adoption: the back button of `NavigationStack`.
struct BackLinkSection: CataloguePage {
    static let title = "Back link (NavigationStack back button)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Text("The back button at the top of this page is the system's. Its label is the title of the previous screen.")
            .monarchTextStyle(.body)

        CatalogueCaption("A pushed screen gets a back button without any code")
        NavigationLink("Open example.com") {
            Text("The back button reads the title of the page you came from.")
                .monarchTextStyle(.body)
                .padding(theme.spacing.space4)
                .navigationTitle("example.com")
        }
        .buttonStyle(.monarch())

        CatalogueCaption("Going back from code: the dismiss action")
        MonarchButton("Go back", systemImage: "chevron.backward") { dismiss() }
    }
}
