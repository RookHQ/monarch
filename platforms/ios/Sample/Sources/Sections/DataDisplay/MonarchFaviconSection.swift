import MonarchUI
import SwiftUI

struct MonarchFaviconSection: CataloguePage {
    static let title = "Favicon"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Letter tile, in every size")
        HStack(spacing: theme.spacing.space3) {
            ForEach(MonarchFaviconSize.allCases, id: \.self) { size in
                MonarchFavicon(image: nil, name: "example.com", size: size)
            }
        }
        CatalogueCaption("Image, in every shape")
        HStack(spacing: theme.spacing.space3) {
            ForEach(MonarchFaviconShape.allCases, id: \.self) { shape in
                MonarchFavicon(image: Image(systemName: "globe"), name: "Globe", size: .lg, shape: shape)
            }
        }
        CatalogueCaption("Beside the name of the site")
        ForEach(["monarchui.dev", "https://www.example.org/pricing", "9to5.example"], id: \.self) { site in
            HStack(spacing: theme.spacing.space2) {
                MonarchFavicon(image: nil, name: site, size: .md)
                Text(site).monarchTextStyle(.body)
            }
        }
        CatalogueCaption("From a URL (the letter shows until it loads, and if it fails)")
        MonarchFavicon(
            url: URL(string: "https://www.apple.com/favicon.ico"),
            name: "apple.com",
            label: "Apple",
            size: .lg
        )
    }
}
