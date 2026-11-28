import MonarchUI
import SwiftUI

struct MonarchLinkStyleSection: CataloguePage {
    static let title = "Link"

    @Environment(\.monarchTheme) private var theme
    @State private var taps = 0

    var body: some View {
        CatalogueCaption("Variants on a Button. Taps: \(taps)")
        ForEach(MonarchLinkVariant.allCases, id: \.self) { variant in
            Button(String(describing: variant)) { taps += 1 }
                .buttonStyle(.monarchLink(variant))
                .monarchTextStyle(.body)
        }

        CatalogueCaption("A SwiftUI Link that leaves the app")
        if let url = URL(string: "https://www.apple.com/accessibility/") {
            Link("Accessibility on apple.com", destination: url)
                .buttonStyle(.monarchLink(isExternal: true))
                .monarchTextStyle(.body)
        }

        CatalogueCaption("Under a field, at caption size")
        Button("Forgot password?") { taps += 1 }
            .buttonStyle(.monarchLink(.subtle))
            .monarchTextStyle(.caption)

        CatalogueCaption("Disabled")
        Button("Billing settings") {}
            .buttonStyle(.monarchLink(.standalone))
            .monarchTextStyle(.body)
            .disabled(true)

        CatalogueCaption("A link inside a sentence is Markdown in Text, tinted with the link colour")
        Text("Read the [terms of service](https://example.com/terms) before you continue.")
            .monarchTextStyle(.body)
            .tint(theme.colors.textLink)
    }
}
